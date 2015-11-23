package mujmail.ui;

/*
MujMail - Simple mail client for J2ME
Copyright (C) 2005 Pavel Machek <pavel@ucw.cz>
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import java.io.*;

/**
 * Provides functions to play audio alerts.
 * 
 * @author Pavel Machek
 */
public class AudioAlert extends Object implements Runnable {

    static long lastRun;

    /**
     * Creates the instance of calss AudioAlert.
     */
    public AudioAlert() {
        synchronized (this) {
            //don't play anything in a short time at all, it may crash Java :)
            if (System.currentTimeMillis() - lastRun <= 5000) {
                return;
            }
            lastRun = System.currentTimeMillis();
        }
        new Thread(this).start();
    }

    /**
     * Plays alert using wav format.
     */
    public void wav() {
        /* Unfortunately, this does not seem to work on 6230 */

        InputStream is = getClass().getResourceAsStream("/sounds/newmail.wav");
        System.out.println(is);
        String ctype = "audio/x-wav";
        try {
            javax.microedition.media.Player player =
                    javax.microedition.media.Manager.createPlayer(is, ctype);

            player.realize();
            player.start();
        } catch (Exception ex) {
            System.out.println("could not play wav");
        }

    }

    /**
     * Plays alert using midi audio format.
     */
    public void midi() {
        /* Works ok on both k700i and 6230 */
        InputStream is = getClass().getResourceAsStream("/sounds/newmail.mid");
        System.out.println(is);
        String ctype = "audio/midi";
        try {
            javax.microedition.media.Player player =
                    javax.microedition.media.Manager.createPlayer(is, ctype);

            player.realize();
            player.start();
        } catch (Exception ex) {
            System.out.println("could not play midi");
        }

    }

    /**
     * Plays alert using mp3 audio format.
     */
    public void mp3() {

        InputStream is = getClass().getResourceAsStream("/sounds/newmail.mp3");
        System.out.println(is);
        String ctype = "audio/mpeg";
        try {
            javax.microedition.media.Player player =
                    javax.microedition.media.Manager.createPlayer(is, ctype);

            player.realize();
            player.start();
        } catch (Exception ex) {
            System.out.println("could not play mp3");
        }

    }

    /**
     * Plays alert using tone.
     */
    public void tone() {
        try {
            javax.microedition.media.Manager.playTone(60, 200, 90);
            Thread.sleep(200);
            javax.microedition.media.Manager.playTone(70, 200, 90);
            Thread.sleep(200);
            javax.microedition.media.Manager.playTone(60, 200, 90);
        } catch (Exception ex) {
            System.out.println("could not play tone");
        }
    }

    public void run() {
        midi();
    }
}
