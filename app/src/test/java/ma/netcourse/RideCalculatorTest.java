package ma.netcourse;

import static org.junit.Assert.*;
import org.junit.Test;

public class RideCalculatorTest {
    @Test public void screenshotExampleUsesBothDistances() {
        RideCalculator.Config cfg = new RideCalculator.Config(10.5, 5.2, 10, 0.4, 0, 2.0);
        RideCalculator.Evaluation e = RideCalculator.calculate(16, 3.8, 3.9, cfg);
        assertEquals(7.7, e.totalKm, 0.0001);
        assertEquals(7.1158, e.netDh, 0.0001);
        assertEquals(26, e.minimumPriceDh);
        assertFalse(e.viable);
    }
    @Test public void returnLegIsCounted() {
        RideCalculator.Config cfg = new RideCalculator.Config(10, 5, 0, 0, 2, 1);
        RideCalculator.Evaluation e = RideCalculator.calculate(15, 3, 5, cfg);
        assertEquals(10, e.totalKm, 0.0001);
        assertEquals(10, e.netDh, 0.0001);
        assertEquals(15, e.minimumPriceDh);
    }
}
