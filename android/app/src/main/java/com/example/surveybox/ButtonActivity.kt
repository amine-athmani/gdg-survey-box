/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.surveybox

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.android.things.pio.Gpio
import com.google.firebase.FirebaseApp
import com.google.android.things.contrib.driver.pwmspeaker.Speaker
import com.google.android.things.contrib.driver.apa102.Apa102





/**
 * This is the activity that manages button presses and connectivity to Firebase
 */

class ButtonActivity : Activity() {
    private val TAG = ButtonActivity::class.java.simpleName

    private lateinit var databaseManager: DatabaseManager

    var segment = RainbowHat.openDisplay()

    private lateinit var buttons: MutableList<Button>
    private lateinit var statusLedRed: Gpio
    private lateinit var statusLedBlue: Gpio
    private lateinit var statusLedGreen: Gpio

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout);
        Log.i(TAG, "Starting ButtonActivity")
        Log.i(TAG, "Registering gpio drivers")
        initializeGPIO()
        showStarter()
        initializeFirebase()
    }

    private fun initializeGPIO() {
        Log.i(TAG, "Configuring GPIO pins")
        // Configure the GPIO Pins for the leds
        Log.i(TAG, "Configuring LED pins")
        statusLedRed = RainbowHat.openLedRed()
        statusLedBlue = RainbowHat.openLedBlue()
        statusLedGreen = RainbowHat.openLedGreen()

        // Configure the buttons to emit key events on GPIO state changes
        buttons = mutableListOf(RainbowHat.openButtonA(),
                RainbowHat.openButtonB(),
                RainbowHat.openButtonC())
        for ((i, button) in buttons.withIndex()) {
            Log.i(TAG, "Registering button ${Integer.toString(i)}")

            // Create GPIO connection.
            button.setDebounceDelay(10)
            button.setOnButtonEventListener { _, pressed ->
                run {
                    if (!pressed) {
                        Log.i(TAG,"Button" + i + "Pressed")
                        if (i==0) {
                            blinkStatusLed(statusLedRed)
                            segment.display("SAD")
                        }
                        else if (i==1) {
                            blinkStatusLed(statusLedGreen)
                            segment.display("NEUT")
                            Thread.sleep(500)
                            segment.display("EUTR")
                            Thread.sleep(500)
                            segment.display("UTRA")
                            Thread.sleep(500)
                            segment.display("TRAL")
                            Thread.sleep(500)
                        }
                        else {
                            blinkStatusLed(statusLedBlue)
                            segment.display("HAPP")
                            Thread.sleep(500)
                            segment.display("APPY")
                            Thread.sleep(500)
                        }
                        databaseManager.addButtonPress(i)
                    }
                }
            }
        }
    }

    private fun initializeFirebase() {
        FirebaseApp.initializeApp(this)
        databaseManager = DatabaseManager()
    }

    private fun blinkStatusLed(statusLed : Gpio) {
        statusLed.value = true
        Handler().postDelayed({
            statusLed.value = false
        }, 250)
    }

    override fun onDestroy() {
        super.onDestroy()
        for (button in buttons) {
            button.close()
            buttons.remove(button)
        }
        statusLedRed.close()
        statusLedBlue.close()
        statusLedGreen.close()
        segment.close()
    }

    private fun showStarter() {
        segment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
        segment.display("ABCD")
        segment.setEnabled(true)
        segment.apply {
            (3 downTo 1).forEach {
                segment.display(it)
                Thread.sleep(800)
            }.also {
                segment.display(" GO ")
            }
        }
    }
}