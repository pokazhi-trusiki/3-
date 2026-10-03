package com.example

import com.example.data.SensorLogPoint
import com.example.sensor.ColorPalette
import com.example.sensor.HeatmapMetric
import com.example.ui.heatmap3d.HeatmapGridCalculator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testHeatmapGridCalculator() {
    val samplePoints = listOf(
      SensorLogPoint(
        id = 1,
        sessionId = 1,
        timestamp = 1000L,
        latitude = 55.75,
        longitude = 37.61,
        altitude = 150.0,
        accuracy = 3.0f,
        speedKmh = 4.0f,
        bearing = 90.0f,
        relX = 0f,
        relY = 0f,
        accelMagnitude = 9.8f,
        accelX = 0f,
        accelY = 0f,
        accelZ = 9.8f,
        gyroMagnitude = 0.5f,
        gyroX = 0f,
        gyroY = 0f,
        gyroZ = 0.5f,
        magMagnitude = 45f,
        magX = 20f,
        magY = 10f,
        magZ = 38f,
        lightLux = 300f,
        pressureHpa = 1013.25f,
        proximityCm = 5f,
        pitchDeg = 0f,
        rollDeg = 0f,
        azimuthDeg = 90f,
        stepCount = 10
      ),
      SensorLogPoint(
        id = 2,
        sessionId = 1,
        timestamp = 2000L,
        latitude = 55.7501,
        longitude = 37.6101,
        altitude = 151.0,
        accuracy = 3.0f,
        speedKmh = 5.0f,
        bearing = 90.0f,
        relX = 10f,
        relY = 10f,
        accelMagnitude = 14.2f,
        accelX = 2f,
        accelY = 3f,
        accelZ = 13.5f,
        gyroMagnitude = 1.2f,
        gyroX = 0.2f,
        gyroY = 0.5f,
        gyroZ = 1.0f,
        magMagnitude = 85f,
        magX = 40f,
        magY = 30f,
        magZ = 68f,
        lightLux = 550f,
        pressureHpa = 1012.0f,
        proximityCm = 5f,
        pitchDeg = 5f,
        rollDeg = 2f,
        azimuthDeg = 95f,
        stepCount = 25
      )
    )

    val grid = HeatmapGridCalculator.computeGrid(
      points = samplePoints,
      metric = HeatmapMetric.MAGNETIC_FIELD,
      gridResolution = 10
    )

    assertEquals(10, grid.gridCols)
    assertEquals(10, grid.gridRows)
    assertEquals(100, grid.cells.size)
    assertTrue("Max value should be at least 85", grid.maxValue >= 85f)
    assertTrue("Min value should be around 45", grid.minValue <= 45f)

    val color = HeatmapGridCalculator.interpolateColor(ColorPalette.CYBER_NEON, 0.5f)
    assertNotNull(color)
  }
}

