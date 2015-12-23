package ru.lj.alamar.microbe;

import java.util.Collections;
import java.util.Random;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

public class ChromosomeSelection {
    public static float total = 0;
    public static float alive = 0;
    public static float taint = 0;
    public static float alter = 0;

    public static void main(String[] args) {
        Random r = new XorShiftRandom(12345);
        for (int i = 2; i < args.length; i++) {
            simulate(r, args[i], Integer.parseInt(args[1]), Integer.parseInt(args[0]));
        }
        System.err.println("total: " + (int)total + ", alive: " + (alive / total)
                + ", tainted: " + (taint / alive) + ", altered: " + (alter / alive));
    }

    private static void simulate(Random r, String g, int steps, int generations) {
        --generations;
        for (int i = 0; i < steps; i++) {
            total++;
            ListF<String> doubled = Cf.arrayList(Cf.list((g + g).split("")));
            Collections.shuffle(doubled, r);
            String result = Cf.list(doubled.mkString("").substring(0, g.length()).split("")).sort().mkString("");
            if (result.indexOf('A') < 0 && result.indexOf('a') < 0) continue;
            if (result.indexOf('B') < 0) continue;
            System.out.println(result);
            if (generations > 0) simulate(r, result, steps / 2, generations);
            alive++;
            if (result.indexOf('-') >= 0) taint++;
            if (result.indexOf('a') >= 0) alter++;
        }
    }
}
