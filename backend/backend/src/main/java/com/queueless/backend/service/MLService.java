//package com.queueless.backend.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//
//import javax.script.ScriptEngine;
//import javax.script.ScriptEngineManager;
//import javax.script.ScriptException;
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//
//@Service
//public class MLService {
//
//    @Value("${ml.model.path}")
//    private String modelPath;
//
//    /**
//     * Get wait time prediction from the model
//     *
//     * @param queueId - Queue ID for which the prediction is needed
//     * @param currentLoad - Current load (number of people waiting)
//     * @param hourOfDay - Current hour of the day (24-hour format)
//     * @return Predicted wait time in minutes
//     */
//    public double predictWaitTime(String queueId, int currentLoad, int hourOfDay) {
//        try {
//            ProcessBuilder processBuilder = new ProcessBuilder("python", "python_model/ml_wait_time_predictor.py",
//                    String.valueOf(currentLoad), String.valueOf(hourOfDay));
//            processBuilder.redirectErrorStream(true);
//            Process process = processBuilder.start();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String output = reader.readLine();
//
//            if (StringUtils.hasText(output)) {
//                return Double.parseDouble(output);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return -1; // Return -1 if prediction failed
//    }
//}
