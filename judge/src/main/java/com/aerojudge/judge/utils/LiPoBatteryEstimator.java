package com.aerojudge.judge.utils;

public class LiPoBatteryEstimator {

    private int cells; // 2 or 3
    private final double[] voltagePointsPerCell;
    private final double[] percentPoints;

    public LiPoBatteryEstimator() {
        this.cells = 0; // Not yet set - detected later

        // Approximate LiPo discharge curve per cell (V → %)
        voltagePointsPerCell = new double[] {
            4.20, 4.00, 3.90, 3.85, 3.80,
            3.75, 3.70, 3.65, 3.60, 3.50,
            3.40, 3.30, 3.20
        };
        percentPoints = new double[] {
            100, 95, 85, 75, 65,
             60, 50, 40, 35, 20,
             12,  6,  0
        };
    }

    /**
     * Auto-detect battery type (2S or 3S) based on measured pack voltage.
     * If >= 9.0 V → assume 3S, otherwise 2S.
     */
    private void detectCells(double packVoltage) {
        if (cells == 0) {
            if (packVoltage >= 9.0) {
                this.cells = 3;
            } else {
                this.cells = 2;
            }
        }
    }

    /**
     * Estimate LiPo battery percentage.
     * @param packVoltage measured pack voltage (V)
     * @return percentage (0-100)
     */
    public int estimatePercentage(double packVoltage) {
        if (Double.isNaN(packVoltage) || Double.isInfinite(packVoltage)) return 0;

        detectCells(packVoltage); //dermine number of cells if not yet set

        //Convert to per-cell voltage
        double perCell = packVoltage / cells;

        // Clamp top/bottom
        if (perCell >= voltagePointsPerCell[0]) return 100;
        if (perCell <= voltagePointsPerCell[voltagePointsPerCell.length - 1]) return 0;

        // Interpolate between known points
        double pct = 0.0;
        for (int i = 0; i < voltagePointsPerCell.length - 1; i++) {
            double vHigh = voltagePointsPerCell[i];
            double vLow = voltagePointsPerCell[i + 1];
            if (perCell <= vHigh && perCell >= vLow) {
                double pHigh = percentPoints[i];
                double pLow = percentPoints[i + 1];
                double frac = (perCell - vLow) / (vHigh - vLow);
                pct = pLow + frac * (pHigh - pLow);
                break;
            }
        }

        //return percentage limited to 0..100
        return (int) Math.max(0, Math.min(100, pct));
    }

    public int getNumCells() {
        return cells;
    }
}
