package com.aerojudge.judge.utils;

import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;
import com.pi4j.context.Context;

public class INA226PowerUtils {

    private static final int INA226_I2C_ADDR = 0x40; // default I2C address, may vary
    private static final int REG_SHUNT_VOLTAGE = 0x01;
    private static final int REG_BUS_VOLTAGE   = 0x02;
    private static final int REG_POWER         = 0x03;
    private static final int REG_CURRENT       = 0x04;

    private I2C device;

    public INA226PowerUtils(Context pi4j) throws Exception {
        I2CProvider i2cProvider = pi4j.provider("linuxfs-i2c");
        I2CConfig config = I2C.newConfigBuilder(pi4j)
                .id("INA226")
                .bus(1)              // I2C bus 1 on Raspberry Pi
                .device(INA226_I2C_ADDR) // sensor address
                .build();
        device = i2cProvider.create(config);
    }

    private int readRegister(int reg) throws Exception {
        byte[] buffer = new byte[2];
        device.readRegister(reg, buffer);
        // INA226 returns MSB first
        return ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
    }

    public double getBusVoltage() throws Exception {
        int raw = readRegister(REG_BUS_VOLTAGE);
        // LSB = 1.25 mV
        return raw * 1.25e-3; // volts
    }

    public double getShuntVoltage() throws Exception {
        int raw = readRegister(REG_SHUNT_VOLTAGE);
        // LSB = 2.5 µV
        return raw * 2.5e-6; // volts
    }

    public double getCurrent() throws Exception {
        int raw = readRegister(REG_CURRENT);
        // This depends on calibration you write to INA226.
        // Assume LSB = 1 mA for now (you must configure properly).
        return raw * 0.001; // amps
    }

    public double getPower() throws Exception {
        int raw = readRegister(REG_POWER);
        // Power LSB = 25 * Current_LSB (depends on calibration).
        // If Current_LSB = 1mA, then Power_LSB = 25mW
        return raw * 0.025; // watts
    }
}

