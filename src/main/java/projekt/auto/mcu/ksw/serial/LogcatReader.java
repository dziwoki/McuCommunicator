package projekt.auto.mcu.ksw.serial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;

public class LogcatReader {
    private final McuCommunicator.McuAction callback;
    private Thread readerThread;
    private Process logProc;
    private boolean isReading = false;

    public LogcatReader(McuCommunicator.McuAction callbackEvent) {
        this.callback = callbackEvent;
    }

    public void startReading() {
        if (isReading) return;
        readerThread = new Thread(() -> {
            try {
                Runtime.getRuntime().exec("logcat -c\n");
                logProc = Runtime.getRuntime().exec("logcat KswMcuListener:I *:S");
                isReading = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedReader bufRead = new BufferedReader(new InputStreamReader(logProc.getInputStream()));
            String line = "";
            try {
                while (isReading) {
                    try {
                        while (bufRead.ready()) {
                            line = bufRead.readLine();
                            if (line.contains("--Mcu toString-----")) {
                                line = line.substring(line.lastIndexOf('[') + 2, line.lastIndexOf(']') - 1);

                                line = line.replaceAll("\\s+", "");
                                String[] splitString = line.split("-", 2);
                                String commandStr = splitString[0].substring(splitString[0].indexOf(":")+1);
                                int command = Integer.parseInt(commandStr,16);
                                String byteStrs = splitString[1].substring(splitString[1].indexOf(":")+1);
                                String[] dataStrs = byteStrs.split("-");
                                byte[] data = new byte[dataStrs.length];
                                for (int i=0; i<data.length; i++) {
                                    data[i] = Byte.parseByte(dataStrs[i], 16);
                                }
                                System.out.println(command);
                                for (Byte b : data)
                                    System.out.println(b);

                                callback.update(command, data);
                            }
                        }
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedIOException e) {
                        break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readerThread.start();
    }

    public void stopReading() {
        logProc.destroy();
        isReading = false;
    }
}
