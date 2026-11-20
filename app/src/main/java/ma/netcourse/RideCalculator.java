package ma.netcourse;

final class RideCalculator {
    static final class Config {
        final double dieselDhPerL, consumptionL100, commissionPercent;
        final double wearDhPerKm, returnKm, minNetDhPerKm;
        Config(double diesel, double consumption, double commission, double wear, double back, double tkm) {
            this.dieselDhPerL = diesel;
            this.consumptionL100 = consumption;
            this.commissionPercent = commission;
            this.wearDhPerKm = wear;
            this.returnKm = back;
            this.minNetDhPerKm = tkm;
        }
    }
    static final class Evaluation {
        final double totalKm, netDh, netDhPerKm;
        final int minimumPriceDh;
        final boolean viable;
        Evaluation(double total, double net, double perKm, int min, boolean viable) {
            this.totalKm=total; this.netDh=net; this.netDhPerKm=perKm;
            this.minimumPriceDh=min; this.viable=viable;
        }
    }
    static Evaluation calculate(double priceDh, double approachKm, double rideKm, Config c) {
        if (!Double.isFinite(priceDh) || priceDh < 0 || !Double.isFinite(approachKm)
            || approachKm < 0 || !Double.isFinite(rideKm) || rideKm <= 0) {
            throw new IllegalArgumentException("Distance ou prix invalide");
        }
        double total = approachKm + rideKm + Math.max(0, c.returnKm);
        if (total <= 0) throw new IllegalArgumentException("Distance nulle");
        double commissionRate = Math.max(0, Math.min(99.0, c.commissionPercent)) / 100.0;
        double costPerKm = Math.max(0, c.dieselDhPerL) * Math.max(0, c.consumptionL100) / 100.0
            + Math.max(0, c.wearDhPerKm);
        double net = priceDh * (1.0 - commissionRate) - total * costPerKm;
        double perKm = net / total;
        int minPrice = (int) Math.ceil(total * (Math.max(0, c.minNetDhPerKm) + costPerKm)
            / (1.0 - commissionRate) - 1e-9);
        return new Evaluation(total, net, perKm, Math.max(0, minPrice), priceDh >= minPrice);
    }
}
