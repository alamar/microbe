package ru.lj.alamar.microbe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Random;
import java.util.Collections;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;

/**
 * @author ilyak
 */
public class TryMain {

    public static void main(String[] args) throws Exception {
        Random r = new Random(444556354);
        ListF<String> collection = Cf.list("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m");
        for (int i = 0; i < 10000; i++) {
            ListF<String> cur = Cf.arrayList(collection);
            Collections.shuffle(cur, r);
            System.out.println(/*takeRandomN(r, cur, 12)*/cur.mkString(""));
        }
    }

    private static ListF<String> takeRandomN(Random r, ListF<String> original, int n) {
        ListF<String> result = Cf.arrayList();
        String[] chromosomes = original.toArray(new String[0]);
        for (int i = 0; i < n; i++) {
            int idx = r.nextInt(n - i);
            String chromosome = chromosomes[idx];
            result.add(chromosome);
            chromosomes[idx] = chromosomes[n - i - 1];
        }
        return result;
    }
}
