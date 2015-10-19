package ru.lj.alamar.microbe;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.Arrays;
import java.util.Set;

/**
 * @author ilyak
 * Some tests depend on Random implementation, sorry about that.
 */
public class MicrobeTest {
    @Test
    public void testNew() {
        Microbe microbe = new Microbe(1, 10);
        assertEquals(1f, microbe.fitness(), 0.001f);
        float[][] chromosomes = microbe.getChromosomes();
        assertEquals(1, chromosomes.length);
        assertEquals(10, chromosomes[0].length);
        assertEquals(0.9f, chromosomes[0][3], 0.001f);
    }

    @Test
    public void testMutate() {
        Microbe microbe = new Microbe(1, 10);
        Random r = new Random(0);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.05f);
        assertEquals(0.092f, microbe.fitness(), 0.001f);
        assertArrayEquals(new float[] {0.9f, 0.9f, 0.4f, 0.9f, 0.4f, 0.4f, 0.9f, 0.9f, 0.95f, 0.9f}, microbe.getChromosomes()[0], 0.001f);

        Microbe offspring = microbe.replicate(r, true);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.05f);
        assertEquals(0.092f, offspring.fitness(), 0.001f);
        assertArrayEquals(new float[] {0.9f, 0.9f, 0.4f, 0.9f, 0.4f, 0.4f, 0.9f, 0.9f, 0.95f, 0.9f}, offspring.getChromosomes()[0], 0.001f);

        assertNotEquals(0.092f, microbe.fitness(), 0.001f);
    }

    @Test
    public void testPloidy() {
        Microbe microbe = new Microbe(6, 10);
        Random r = new Random(0);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.05f);

        Set<String> chromosomes = Cf.hashSet();
        for (float[] chromosome : microbe.getChromosomes()) chromosomes.add(Arrays.toString(chromosome));
        Set<String> chromosomesCopy = Cf.hashSet(chromosomes);

        assertEquals(1.383f, microbe.fitness(), 0.001f);
        assertArrayEquals(new float[] {0.9f, 0.9f, 0.4f, 0.9f, 0.4f, 0.4f, 0.9f, 0.9f, 0.95f, 0.9f}, microbe.getChromosomes()[0], 0.001f);
        assertArrayEquals(new float[] {0.9f, 0.9f, 0.9f, 0.4f, 0.95f, 0.95f, 0.9f, 0.4f, 0.95f, 0.9f}, microbe.getChromosomes()[1], 0.001f);
        Microbe offspring = microbe.replicate(r, false);
        for (int i = 0; i < 6; i++) {
            float[] chromosome1 = offspring.getChromosomes()[i];
            float[] chromosome2 = microbe.getChromosomes()[i];
            if (i != 5) assertNotEquals(Arrays.toString(chromosome1), Arrays.toString(chromosome2));
            if (!chromosomes.remove(Arrays.toString(chromosome1))) assertTrue(chromosomesCopy.remove(Arrays.toString(chromosome1)));
            if (!chromosomes.remove(Arrays.toString(chromosome2))) assertTrue(chromosomesCopy.remove(Arrays.toString(chromosome2)));
        }
        assertTrue(chromosomes.isEmpty());
        assertTrue(chromosomesCopy.isEmpty());
    }
}
