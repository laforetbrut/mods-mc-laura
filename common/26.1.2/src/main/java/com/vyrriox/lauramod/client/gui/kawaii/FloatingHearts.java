package com.vyrriox.lauramod.client.gui.kawaii;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Little hearts and sparkles floating up behind the menu.
 *
 * @author vyrriox
 */
public final class FloatingHearts {
    private record Particle(float x, float y, float speed, float wobble, int size, int color, boolean sparkle, float phase) {
    }

    private final List<Particle> particles = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private float time;
    private float spawn;

    public void render(KDraw d, int x0, int y0, int x1, int y1, float delta) {
        time += delta;
        spawn += delta;
        while (spawn > 6 && particles.size() < 28) {
            spawn -= 6;
            int[] colors = {0x70FFB6D5, 0x60D9C8FF, 0x60FFD6B8, 0x70FF9CC6, 0x60C4F2E2};
            particles.add(new Particle(x0 + random.nextFloat() * (x1 - x0), y1 + 8, 0.25F + random.nextFloat() * 0.45F,
                    random.nextFloat() * 6.28F, 1 + random.nextInt(2), colors[random.nextInt(colors.length)], random.nextInt(4) == 0, random.nextFloat() * 100));
        }
        Iterator<Particle> it = particles.iterator();
        List<Particle> moved = new ArrayList<>();
        while (it.hasNext()) {
            Particle p = it.next();
            it.remove();
            float ny = p.y - p.speed * delta;
            if (ny < y0 - 12) {
                continue;
            }
            moved.add(new Particle(p.x, ny, p.speed, p.wobble, p.size, p.color, p.sparkle, p.phase));
        }
        particles.addAll(moved);
        for (Particle p : particles) {
            int px = (int) (p.x + Math.sin(time * 0.05F + p.wobble) * 6);
            int py = (int) p.y;
            if (p.sparkle) {
                int c = p.color | 0x40000000;
                boolean big = ((int) (time + p.phase) / 10) % 2 == 0;
                d.fill(px, py - (big ? 2 : 1), px + 1, py + (big ? 3 : 2), c);
                d.fill(px - (big ? 2 : 1), py, px + (big ? 3 : 2), py + 1, c);
            } else {
                Kawaii.heart(d, px, py, p.size, p.color);
            }
        }
    }
}
