package ru.lj.alamar.microbe;

import java.util.Random;

/**
 * 
 * @author ilyak
 *
 * http://www.javamex.com/tutorials/random_numbers/java_util_random_subclassing.shtml
 */
public class XorShiftRandom extends Random {
    private long acc;

    public XorShiftRandom(long seed) {
        this.acc = seed;
    }
    protected int next(int nbits) {
      // N.B. Not thread-safe!
      long x = this.acc;
      x ^= (x << 21);
      x ^= (x >>> 35);
      x ^= (x << 4);
      this.acc = x;
      x &= ((1L << nbits) - 1);
      return (int) x;
    }
}

