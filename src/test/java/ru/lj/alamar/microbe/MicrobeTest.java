package ru.lj.alamar.microbe;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;
import java.util.Arrays;
import java.util.Set;

/**
 * @author ilyak
 * Some tests depend on Random implementation, sorry about that.
 */
public class MicrobeTest {
    private void assertArrayNotEquals(float[] expecteds, float[] actuals, float delta) {
        try {
            assertArrayEquals(expecteds, actuals, delta);
        } catch (Error e) {
            return;
        }
        fail("The arrays are equal");
    }

    @Test
    public void testNew() {
        Microbe microbe = new Microbe(0.99f, 1, 10, false);
        assertEquals(1f, microbe.fitness(), 0.001f);
        float[][] chromosomes = microbe.getChromosomes();
        assertEquals(1, chromosomes.length);
        assertEquals(10, chromosomes[0].length);
        assertEquals(0.99f, chromosomes[0][3], 0.001f);
    }

    @Test
    public void testMutate() {
        Microbe microbe = new Microbe(0.99f, 1, 10, false);
        Random r = new Random(0);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertEquals(0.125f, microbe.fitness(), 0.001f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, microbe.getChromosomes()[0], 0.0005f);

        Microbe offspring = microbe.replicate(r, true, 10, 0f);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertEquals(0.125f, offspring.fitness(), 0.001f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, offspring.getChromosomes()[0], 0.0005f);
        assertTrue(microbe.isDead());

        assertNotEquals(0.125f, microbe.fitness(), 0.001f);
    }

    @Test
    public void testSelect() {
        Random r = new Random(0);
        Microbe good = new Microbe(0.99f, 1, 10, false);
        assertEquals(1f, good.fitness(), 0.01f);

        Microbe bad = new Microbe(0.99f, 1, 10, false);
        bad.mutate(r, 0.33f, 0.2f, 0.4f, 0.1f);
        assertEquals(0.5125f, bad.fitness(), 0.0005f);

        Microbe dead = new Microbe(0.99f, 1, 10, false);
        dead.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertEquals(0.2507f, dead.fitness(), 0.0005f);

        assertEquals(0.5125f, Microbe.selectOffspring(r, Cf.list(bad), 0.3f, 1, false, 0f, false).single().fitness(), 0.0005f);
        assertEquals(3, Microbe.selectOffspring(r, Cf.list(bad, good, dead), 0.3f, 1, false, 0f, false).size());
        assertEquals(1f, Microbe.selectOffspring(r, Cf.list(bad, good), 0.3f, 1, false, 0f, false).shuffle().first().fitness(), 0.0005f);
    }
/*
    @Test
    public void testPloidy() {
        Microbe microbe = new Microbe(0.99f, 6, 10, false);
        Random r = new Random(0);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);

        Set<String> chromosomes = Cf.hashSet();
        for (float[] chromosome : microbe.getChromosomes()) chromosomes.add(Arrays.toString(chromosome));
        Set<String> chromosomesCopy = Cf.hashSet(chromosomes);

        assertEquals(1.006f, microbe.fitness(), 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, microbe.getChromosomes()[0], 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.99f, 0.495f, 0.991f, 0.991f, 0.99f, 0.495f, 0.991f, 0.99f}, microbe.getChromosomes()[1], 0.0005f);
        Microbe offspring = microbe.replicate(r, false, 0, 0f);
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

    @Test
    public void testDownsize() {
        Microbe microbe = new Microbe(0.99f, 2, 10, true);
        Random r = new Random(0);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);

        Microbe sibling = microbe.replicate(r, false, 10, 1.0f);
        assertEquals(1, microbe.getChromosomes().length);
        assertEquals(1, sibling.getChromosomes().length);
        assertArrayNotEquals(microbe.getChromosomes()[0], sibling.getChromosomes()[0], 0.0005f);
    }

    @Test
    public void testConversion() {
        Microbe microbe = new Microbe(0.99f, 2, 10, false);
        Random r = new Random(0);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);

        assertEquals(1.003f, microbe.fitness(), 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, microbe.getChromosomes()[0], 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.99f, 0.495f, 0.991f, 0.991f, 0.99f, 0.495f, 0.991f, 0.99f}, microbe.getChromosomes()[1], 0.0005f);

        microbe.conversion(r);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, microbe.getChromosomes()[0], 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.99f, 0.99f, 0.495f, 0.991f, 0.99f, 0.495f, 0.991f, 0.99f}, microbe.getChromosomes()[1], 0.0005f);
        assertEquals(0.501f, microbe.fitness(), 0.0005f);
    }

    @Test
    public void testCrossing() {
        Microbe microbe = new Microbe(0.99f, 2, 10, false);
        Random r = new Random(0);
        microbe.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);

        assertEquals(1.003f, microbe.fitness(), 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, microbe.getChromosomes()[0], 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.99f, 0.495f, 0.991f, 0.991f, 0.99f, 0.495f, 0.991f, 0.99f}, microbe.getChromosomes()[1], 0.0005f);

        microbe.crossing(r);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.495f, 0.991f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, microbe.getChromosomes()[0], 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.99f, 0.99f, 0.495f, 0.991f, 0.99f, 0.495f, 0.991f, 0.99f}, microbe.getChromosomes()[1], 0.0005f);
        assertEquals(1.003f, microbe.fitness(), 0.0005f);
    }

    @Test
    public void testSubstitution() {
        Microbe donor = new Microbe(0.99f, 2, 10, false);
        Random r = new Random(0);
        donor.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertEquals(1.003f, donor.fitness(), 0.0005f);

        Microbe receiver = new Microbe(0.99f, 2, 10, false);
        receiver.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertEquals(1.001f, receiver.fitness(), 0.0005f);

        receiver.chromosomeSubstitution(r, donor);
        assertArrayEquals(receiver.getChromosomes()[1], donor.getChromosomes()[1], 0.0005f);

        assertEquals(1.003f, donor.fitness(), 0.0005f);
        assertNotEquals(1.001f, receiver.fitness(), 0.0005f);
    }

    @Test
    public void testExchange() {
        Microbe first = new Microbe(0.99f, 2, 10, false);
        Random r = new Random(0);
        first.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertEquals(1.003f, first.fitness(), 0.0005f);

        Microbe second = new Microbe(0.99f, 2, 10, false);
        second.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertEquals(1.001f, second.fitness(), 0.0005f);

        float[] firstCopy = first.getChromosomes()[1].clone();
        float[] secondCopy = second.getChromosomes()[1].clone();
        second.chromosomeExchange(r, first);
        assertArrayEquals(secondCopy, first.getChromosomes()[1], 0.0005f);
        assertArrayEquals(firstCopy, second.getChromosomes()[1], 0.0005f);

        assertNotEquals(1.003f, first.fitness(), 0.0005f);
        assertNotEquals(1.001f, second.fitness(), 0.0005f);
    }
*/
    @Test
    public void testHorizontalTransfer() {
        Random r = new Random(0);
        Microbe donor = new Microbe(0.99f, 1, 10, false);
        Microbe receiver = new Microbe(0.99f, 1, 10, false);
        donor.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        receiver.mutate(r, 0.33f, 0.5f, 0.4f, 0.1f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, donor.getChromosomes()[0], 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.99f, 0.495f, 0.991f, 0.991f, 0.99f, 0.495f, 0.991f, 0.99f}, receiver.getChromosomes()[0], 0.0005f);
        float donorFitness = donor.fitness();
        float receiverFitness = receiver.fitness();
        receiver.horizontalTransfer(r, donor);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.495f, 0.99f, 0.495f, 0.495f, 0.99f, 0.99f, 0.991f, 0.99f}, donor.getChromosomes()[0], 0.0005f);
        assertArrayEquals(new float[] {0.99f, 0.99f, 0.99f, 0.99f, 0.495f, 0.991f, 0.99f, 0.495f, 0.991f, 0.99f}, receiver.getChromosomes()[0], 0.0005f);
        assertEquals(donorFitness, donor.fitness(), 0.0005f);
        assertNotEquals(receiverFitness, receiver.fitness(), 0.000005f);
    }
}
