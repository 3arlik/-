package ru.threel.automine.logic;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.threel.automine.AutoMineConfig;

import java.util.HashSet;
import java.util.Set;

// Выполняется каждый клиентский тик. Простая (не A*) навигация: идём прямо
// на цель, прыгаем при препятствии. Для открытых пространств этого хватает.
public final class MiningTask {

    private static BlockPos target = null;
    private static Vec3d lastPos = null;
    private static int stuckTicks = 0;
    private static final Set<BlockPos> tempBlacklist = new HashSet<>();
    private static int blacklistTimer = 0;

    private MiningTask() {}

    public static void tick(MinecraftClient client) {
        AutoMineConfig cfg = AutoMineConfig.get();
        ClientPlayerEntity player = client.player;
        if (!cfg.enabled || player == null || client.world == null) return;

        blacklistTimer++;
        if (blacklistTimer > 200) { // раз в 10с чистим временный чёрный список
            tempBlacklist.clear();
            blacklistTimer = 0;
        }

        if (target == null || !isStillValid(client, cfg, target)) {
            target = findNearestTarget(client, cfg);
            stuckTicks = 0;
            lastPos = null;
        }
        if (target == null) return; // ничего не найдено в радиусе — ждём

        double dist = player.getPos().distanceTo(Vec3d.ofCenter(target));
        if (dist <= cfg.reach) {
            mine(client, player, target);
        } else {
            navigateTo(player, target);
            checkStuck(player);
        }
    }

    private static boolean isStillValid(MinecraftClient client, AutoMineConfig cfg, BlockPos pos) {
        if (tempBlacklist.contains(pos)) return false;
        BlockState state = client.world.getBlockState(pos);
        return cfg.isTarget(state.getBlock());
    }

    // сканирование куба вокруг игрока, ближайший подходящий и безопасный блок
    private static BlockPos findNearestTarget(MinecraftClient client, AutoMineConfig cfg) {
        ClientPlayerEntity player = client.player;
        BlockPos origin = player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -cfg.radius; dx <= cfg.radius; dx++) {
            for (int dy = -cfg.radius; dy <= cfg.radius; dy++) {
                for (int dz = -cfg.radius; dz <= cfg.radius; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (tempBlacklist.contains(pos)) continue;
                    BlockState state = client.world.getBlockState(pos);
                    if (!cfg.isTarget(state.getBlock())) continue;
                    if (!isSafeToMine(client, pos)) continue;
                    double d = origin.getSquaredDistance(pos);
                    if (d < bestDist) { bestDist = d; best = pos; }
                }
            }
        }
        return best;
    }

    // не копать пол под собой; не копать если снизу пустота/лава/вода
    private static boolean isSafeToMine(MinecraftClient client, BlockPos pos) {
        ClientPlayerEntity player = client.player;
        if (pos.getY() < player.getBlockPos().getY() - 1) return false;

        BlockPos below = pos.down();
        BlockState belowState = client.world.getBlockState(below);
        if (belowState.isAir()) return false;
        Identifier belowId = Registries.BLOCK.getId(belowState.getBlock());
        for (String hazard : AutoMineConfig.HAZARDS) {
            if (belowId.toString().equals(hazard)) return false;
        }
        // не подходим вплотную к опасностям рядом с целью
        for (Direction dir : Direction.values()) {
            BlockState side = client.world.getBlockState(pos.offset(dir));
            Identifier sideId = Registries.BLOCK.getId(side.getBlock());
            for (String hazard : AutoMineConfig.HAZARDS) {
                if (sideId.toString().equals(hazard)) return false;
            }
        }
        return true;
    }

    private static void navigateTo(ClientPlayerEntity player, BlockPos pos) {
        Vec3d targetCenter = Vec3d.ofCenter(pos);
        Vec3d dir = targetCenter.subtract(player.getPos());
        Vec3d flat = new Vec3d(dir.x, 0, dir.z).normalize();
        double speed = 0.2;

        // поворот в сторону цели
        double yaw = Math.toDegrees(Math.atan2(-flat.x, flat.z));
        player.setYaw((float) yaw);

        player.setVelocity(flat.x * speed, player.getVelocity().y, flat.z * speed);

        // прыжок, если впереди препятствие на уровне ног
        BlockPos ahead = player.getBlockPos().add((int) Math.signum(flat.x), 0, (int) Math.signum(flat.z));
        if (!player.getWorld().getBlockState(ahead).isAir() && player.isOnGround()) {
            player.jump();
        }
    }

    private static void checkStuck(ClientPlayerEntity player) {
        Vec3d cur = player.getPos();
        if (lastPos != null && cur.distanceTo(lastPos) < 0.05) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
        }
        lastPos = cur;
        if (stuckTicks > 15 * 20) { // 15 секунд без прогресса (20 тиков/сек)
            tempBlacklist.add(target);
            target = null;
            stuckTicks = 0;
        }
    }

    private static void mine(MinecraftClient client, ClientPlayerEntity player, BlockPos pos) {
        // смотрим на блок и держим "кнопку" добычи каждый тик
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        client.crosshairTarget = hit;
        if (client.interactionManager != null) {
            client.interactionManager.attackBlock(pos, hit.getSide());
        }
        if (client.world.getBlockState(pos).isAir()) {
            target = null; // добыто — на следующем тике найдём новую цель
        }
    }
}
