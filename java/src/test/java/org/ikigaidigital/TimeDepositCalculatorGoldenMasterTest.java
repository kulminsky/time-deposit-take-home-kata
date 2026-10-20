package org.ikigaidigital;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden master: {@link TimeDepositCalculator} must give bit-for-bit the same balances as the
 * frozen {@link LegacyTimeDepositCalculator} for any deposit, including repeated monthly updates.
 */
class TimeDepositCalculatorGoldenMasterTest {

    private static final long SEED = 20261019L;
    private static final int RANDOM_DEPOSITS = 100_000;
    private static final int MONTHS = 12;

    // Real plan names, case variants and unknown plans (all unknown ones must get no interest).
    private static final String[] PLAN_TYPES =
        {"basic", "student", "premium", "Basic", "STUDENT", "Premium", "gold", "", "basic "};
    private static final int[] BOUNDARY_DAYS = {-1, 0, 1, 29, 30, 31, 44, 45, 46, 364, 365, 366, 367};
    private static final double[] SAMPLE_BALANCES = {0.0, 0.01, 6.00, 18.00, 1000.00, 1234567.89, -1000.00};

    @Test
    void matchesLegacyCalculatorForRandomDepositsOverTwelveMonths() {
        Random random = new Random(SEED);
        List<TimeDeposit> actual = new ArrayList<>();
        List<TimeDeposit> expected = new ArrayList<>();
        for (int id = 0; id < RANDOM_DEPOSITS; id++) {
            String planType = PLAN_TYPES[random.nextInt(PLAN_TYPES.length)];
            int days = random.nextBoolean()
                ? BOUNDARY_DAYS[random.nextInt(BOUNDARY_DAYS.length)]
                : random.nextInt(1000);
            double balance = randomBalance(random);
            actual.add(new TimeDeposit(id, planType, balance, days));
            expected.add(new TimeDeposit(id, planType, balance, days));
        }

        assertSameBalancesForEachMonth(actual, expected);
    }

    @Test
    void matchesLegacyCalculatorForEveryPlanDayAndSampleBalance() {
        List<TimeDeposit> actual = new ArrayList<>();
        List<TimeDeposit> expected = new ArrayList<>();
        int id = 0;
        for (String planType : PLAN_TYPES) {
            for (int days = -1; days <= 800; days++) {
                for (double balance : SAMPLE_BALANCES) {
                    actual.add(new TimeDeposit(id, planType, balance, days));
                    expected.add(new TimeDeposit(id, planType, balance, days));
                    id++;
                }
            }
        }

        assertSameBalancesForEachMonth(actual, expected);
    }

    private static double randomBalance(Random random) {
        switch (random.nextInt(4)) {
            case 0:
                return Math.round(random.nextDouble() * 100_000_000_000d) / 100.0; // whole cents up to 1e9
            case 1:
                return Math.round(random.nextDouble() * 1_000_000d) / 100.0;       // small whole cents
            case 2:
                return random.nextDouble() * 10_000_000d;                           // arbitrary double
            default:
                return -Math.round(random.nextDouble() * 100_000_000d) / 100.0;     // negative whole cents
        }
    }

    private static void assertSameBalancesForEachMonth(List<TimeDeposit> actual, List<TimeDeposit> expected) {
        TimeDepositCalculator calculator = new TimeDepositCalculator();
        LegacyTimeDepositCalculator legacyCalculator = new LegacyTimeDepositCalculator();

        for (int month = 1; month <= MONTHS; month++) {
            calculator.updateBalance(actual);
            legacyCalculator.updateBalance(expected);

            List<String> mismatches = new ArrayList<>();
            for (int i = 0; i < actual.size(); i++) {
                TimeDeposit a = actual.get(i);
                TimeDeposit e = expected.get(i);
                if (Double.doubleToLongBits(a.getBalance()) != Double.doubleToLongBits(e.getBalance())) {
                    mismatches.add(String.format("month %d, plan '%s', %d days: expected %s but was %s",
                        month, e.getPlanType(), e.getDays(), e.getBalance(), a.getBalance()));
                }
            }
            assertThat(mismatches).as("deposits that differ from the legacy calculator").isEmpty();
        }
    }
}
