package ca.usask.chl848.nfc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

/**
 * Created by chl848 on 06/08/2015.
 */
public class MainView extends View {
    Paint m_paint;

    private String m_id;
    private String m_name;
    private int m_color;
    private static final int m_messageTextSize = 50;
    private static final int m_textStrokeWidth = 2;
    private static final int m_boundaryStrokeWidth = 10;

    private String m_message;

    public class Ball {
        public int m_ballColor;
        public float m_ballX;
        public float m_ballY;
        public boolean m_isTouched;
        public String m_id;
        public String m_name;

    }
    private ArrayList<Ball> m_balls;
    private int m_touchedBallId;

    private float m_ballRadius;
    private float m_ballBornX;
    private float m_ballBornY;

    public class RemotePhoneInfo {
        String m_name;
        int m_color;
    }

    private ArrayList<RemotePhoneInfo> m_remotePhones;

    /**
     * experiment begin
     */
    private ArrayList<String> m_ballNames;
    private long m_trailStartTime;
    private int m_maxBlocks;
    private int m_maxTrails;
    private int m_currentBlock;
    private int m_currentTrail;
    private String m_transactionId;

    private static final int m_experimentPhoneNumber = 3;

    private MainLogger m_logger;
    private MainLogger m_receiveLogger;

    private boolean m_isExperimentInitialised;
    /**
     * experiment end
     */

    public MainView (Context context) {
        super(context);

        m_paint = new Paint();
        m_remotePhones = new ArrayList<>();
        setBackgroundColor(Color.WHITE);
        m_message = "No Message";
        m_id = ((MainActivity)(context)).getUserId();
        m_name = ((MainActivity)(context)).getUserName();
        Random rnd = new Random();
        m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        m_touchedBallId = -1;
        m_balls = new ArrayList<>();
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        m_ballRadius = displayMetrics.widthPixels * 0.08f;
        m_ballBornX = displayMetrics.widthPixels * 0.5f;
        m_ballBornY = displayMetrics.heightPixels * 0.75f - m_ballRadius * 2.0f;

        m_isExperimentInitialised = false;

        //addBall();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        showBoundary(canvas);
        showMessage(canvas);
        showBalls(canvas);
        showProgress(canvas);
    }

    public void showBoundary(Canvas canvas) {
        m_paint.setColor(Color.RED);
        m_paint.setStrokeWidth(m_boundaryStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawLine(0, displayMetrics.heightPixels * 0.75f, displayMetrics.widthPixels, displayMetrics.heightPixels * 0.75f, m_paint);
    }

    public void showMessage(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.GREEN);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        canvas.drawText(m_message, displayMetrics.widthPixels * 0.3f, displayMetrics.heightPixels * 0.8f, m_paint);
    }

    public void showBalls(Canvas canvas) {
        if (!m_balls.isEmpty()) {
            for (Ball ball : m_balls) {
                m_paint.setColor(ball.m_ballColor);
                m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(ball.m_ballX, ball.m_ballY, m_ballRadius, m_paint);

                /**
                 * experiment begin
                 */
                m_paint.setStrokeWidth(m_textStrokeWidth);
                float textX = ball.m_ballX - m_ballRadius;
                float textY = ball.m_ballY - m_ballRadius;
                if (ball.m_name.length() > 5) {
                    textX = ball.m_ballX - m_ballRadius * 2.0f;
                }
                canvas.drawText(ball.m_name, textX, textY, m_paint);
                /**
                 * experiment end
                 */
            }
        }
    }

    public void showProgress(Canvas canvas) {
        m_paint.setTextSize(m_messageTextSize);
        m_paint.setColor(Color.BLUE);
        m_paint.setStrokeWidth(m_textStrokeWidth);
        m_paint.setStyle(Paint.Style.FILL_AND_STROKE);
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        String block = "Block: " + m_currentBlock +"/" + m_maxBlocks;
        canvas.drawText(block, (int) (displayMetrics.widthPixels * 0.75), (int) (displayMetrics.heightPixels * 0.1), m_paint);

        String trial = "Trial: " + m_currentTrail +"/" + m_maxTrails;
        canvas.drawText(trial, (int) (displayMetrics.widthPixels * 0.75), (int) (displayMetrics.heightPixels * 0.15), m_paint);
    }

    public int getBallCount() {
        return m_balls.size();
    }

    public void setMessage (String msg) {
        m_message = msg;
    }

    public void updateRemotePhone(String name, int color){
        if (name.isEmpty() || name.equalsIgnoreCase(m_name)) {
            return;
        }

        int size = m_remotePhones.size();
        boolean isFound = false;
        for (int i = 0; i<size; ++i) {
            RemotePhoneInfo info = m_remotePhones.get(i);
            if (info.m_name.equalsIgnoreCase(name)) {
                info.m_color = color;
                isFound = true;
                break;
            }
        }

        if (!isFound) {
            RemotePhoneInfo info = new RemotePhoneInfo();
            info.m_name = name;
            info.m_color = color;

            m_remotePhones.add(info);

            /**
             * experiment end
             */
            if (m_remotePhones.size() == m_experimentPhoneNumber && !m_isExperimentInitialised) {
                initExperiment();
            }
            /**
             * experiment end
             */
        }
    }

    public ArrayList<RemotePhoneInfo> getRemotePhones() {
        return m_remotePhones;
    }

    public void removePhones(ArrayList<RemotePhoneInfo> phoneInfos) {
        m_remotePhones.removeAll(phoneInfos);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        float X = event.getX();
        float Y = event.getY();
        float touchRadius = event.getTouchMajor();

        int ballCount = m_balls.size();
        switch (eventaction) {
            case MotionEvent.ACTION_DOWN:
                if (!canTouch(X, Y)) {
                    break;
                }
                m_touchedBallId = -1;
                for (int i = 0; i < ballCount; ++i){
                    Ball ball = m_balls.get(i);
                    ball.m_isTouched = false;

                    double dist;
                    dist = Math.sqrt(Math.pow((X - ball.m_ballX), 2) + Math.pow((Y - ball.m_ballY), 2));
                    if (dist <= (touchRadius + m_ballRadius)) {
                        ball.m_isTouched = true;
                        m_touchedBallId = i;

                        boolean isOverlap = false;
                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist2 = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist2 <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap && !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }

                    if (m_touchedBallId > -1)
                    {
                        break;
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (m_touchedBallId > -1) {
                    Ball ball = m_balls.get(m_touchedBallId);
                    if (ball.m_isTouched) {
                        boolean isOverlap = false;

                        for (int j = 0; j < ballCount; ++j) {
                            if (j != m_touchedBallId) {
                                Ball ball2 = m_balls.get(j);

                                double dist = Math.sqrt(Math.pow((X - ball2.m_ballX), 2) + Math.pow((Y - ball2.m_ballY), 2));
                                if (dist <= m_ballRadius * 2) {
                                    isOverlap = true;
                                }
                            }
                        }

                        if (!isOverlap && !isBoundary(X, Y)) {
                            ball.m_ballX = X;
                            ball.m_ballY = Y;
                            this.invalidate();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                for (Ball ball : m_balls) {
                    ball.m_isTouched = false;
                }
                break;
        }

        return  true;
    }

    private boolean isBoundary(float x, float y) {
        boolean rt = false;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        if ((y-m_ballRadius <= 0.0f) || (y+m_ballRadius >= (displayMetrics.heightPixels * 0.75f)) || (x-m_ballRadius <= 0.0f) || (x+m_ballRadius >= displayMetrics.widthPixels)) {
            rt = true;
        }

        return rt;
    }

    private boolean canTouch(float x, float y) {
        boolean rt = true;
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();

        if ((y <= 0.0f) || (y >= (displayMetrics.heightPixels * 0.75f)) || (x <= 0.0f) || (x >= displayMetrics.widthPixels)) {
            rt = false;
        }

        return rt;
    }

    public void addBall() {
        Ball ball = new Ball();
        Random rnd = new Random();
        ball.m_ballColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        ball.m_ballX = m_ballBornX;
        ball.m_ballY = m_ballBornY;
        ball.m_isTouched = false;
        ball.m_id = UUID.randomUUID().toString();
        ball.m_name = getBallName();
        m_balls.add(ball);
        this.invalidate();
    }

    public  void removeBall(String id) {
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                m_balls.remove(ball);
                m_touchedBallId = -1;
                break;
            }
        }
    }

    public  void removeBalls() {
        m_balls.clear();
    }

    public void receivedBall(String id, int color) {
        boolean isReceived = false;
        for (Ball ball : m_balls) {
            if (ball.m_id.equalsIgnoreCase(id)) {
                isReceived = true;
                break;
            }
        }

        if (!isReceived) {
            Ball ball = new Ball();
            ball.m_id = id;
            ball.m_ballColor = color;
            ball.m_isTouched = false;

            ball.m_ballX = m_ballBornX;
            ball.m_ballY = m_ballBornY;

            m_balls.add(ball);
        }
    }

    public String encodeMessage() {
        if (!m_balls.isEmpty()) {
            JSONObject jsonObject = new JSONObject();
            Ball ball = m_balls.get(0);
            try {
                jsonObject.put("ballId", ball.m_id);
                jsonObject.put("ballColor", ball.m_ballColor);
                jsonObject.put("receiverName", ball.m_name);
                jsonObject.put("senderName", m_name);
                jsonObject.put("senderId", m_id);
                jsonObject.put("blockId", m_currentBlock);
                jsonObject.put("trialId", m_currentTrail);
                jsonObject.put("transactionId", m_transactionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jsonObject.toString();
        }

        return "";
    }

    public void sendPhoneInfo(){
        JSONObject jo = new JSONObject();
        try {
            jo.put("isSendingBall", false);
            jo.put("name", m_name);
            jo.put("color", m_color);
            jo.put("x", 0);
            jo.put("y", 0);
            jo.put("z", 0);
        } catch (JSONException e){
            e.printStackTrace();
        }

        MainActivity ma = (MainActivity)getContext();
        if (ma != null) {
            ma.addMessage(jo.toString());
        }
    }

    public void clearRemotePhoneInfo() {
        m_remotePhones.clear();
    }

    /**
     * experiment begin
     */
    private void initExperiment() {
        m_isExperimentInitialised = true;

        // init ball names
        m_ballNames = new ArrayList<>();

        m_maxBlocks = 5;
        m_maxTrails = 9;

        m_currentBlock = 0;
        m_currentTrail = 0;

        resetBlock();

        m_logger = new MainLogger(getContext(), m_id+"_"+m_name+"_"+getResources().getString(R.string.app_name));
        //<participantID> <participantName> <condition> <block#> <trial#> <elapsed time for this trial> <transactionId> <timestamp>
        m_logger.writeHeaders("participantID" + "," + "participantName" + "," + "condition" + "," + "block" + "," + "trial" + "," + "elapsedTime" + "," + "transactionId" + "," + "timestamp");

        m_receiveLogger = new MainLogger(getContext(), m_id+"_"+m_name+"_"+getResources().getString(R.string.app_name) + "_" + "receive");
        //<senderID> <senderName> <condition> <block#> <trial#> <receiverName> <actualReceiverName> <isCorrect> <transactionId> <timestamp>
        m_receiveLogger.writeHeaders("senderId" + "," + "senderName" + "," + "condition" + "," + "block" + "," + "trial" + "," + "receiverName" + "," + "actualReceiverName" + "," + "isCorrect" + "," + "transactionId" + "," + "timestamp");

        ((MainActivity) getContext()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((MainActivity) getContext()).setStartButtonEnabled(true);
                ((MainActivity) getContext()).setContinueButtonEnabled(false);
            }
        });
    }

    private String getBallName() {
        if (m_ballNames.isEmpty()) {
            return "";
        }

        Random rnd = new Random();
        int index = rnd.nextInt(m_ballNames.size());
        String name = m_ballNames.get(index);
        m_ballNames.remove(index);
        return name;
    }

    public boolean isFinished() {
        return m_currentBlock == m_maxBlocks;
    }

    public void nextBlock() {
        ((MainActivity)getContext()).setStartButtonEnabled(true);
        ((MainActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void resetBlock() {
        // reset ball names
        m_ballNames.clear();
        for (RemotePhoneInfo remotePhoneInfo : m_remotePhones){
            for(int i=0; i<3; i++){
                m_ballNames.add(remotePhoneInfo.m_name);
            }
        }

        Random rnd = new Random();
        // reset self color
        m_color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        m_transactionId = "";
    }

    public void startBlock() {
        m_currentBlock += 1;
        m_currentTrail = 0;
        resetBlock();
        startTrial();
        ((MainActivity)getContext()).setStartButtonEnabled(false);
        ((MainActivity)getContext()).setContinueButtonEnabled(false);
    }

    public void endBlock() {
        if (isFinished() && (m_logger != null)) {
            closeLogger();
        }

        new AlertDialog.Builder(getContext()).setTitle("Warning").setMessage("You have completed block " + m_currentBlock + ", please wait for other participants.").setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();

        ((MainActivity) getContext()).setContinueButtonEnabled(true);
        ((MainActivity)getContext()).setStartButtonEnabled(false);
        m_currentTrail = 0;
    }

    public void startTrial() {
        m_trailStartTime = System.currentTimeMillis();
        m_currentTrail += 1;
        m_transactionId = UUID.randomUUID().toString();
        addBall();
    }

    public void endTrail() {
        long trailEndTime = System.currentTimeMillis();
        long timeElapse = trailEndTime - m_trailStartTime;

        if (m_currentBlock == 0) {
            ++m_currentBlock;
        }

        if (m_currentTrail == 0) {
            ++m_currentTrail;
        }

        //<participantID> <participantName> <condition> <block#> <trial#> <elapsed time for this trial> <transactionId> <timestamp>
        if (m_logger != null) {
            m_logger.write(m_id + "," + m_name + "," + getResources().getString(R.string.app_name) + "," + m_currentBlock + "," + m_currentTrail + "," + timeElapse + "," + m_transactionId + "," + trailEndTime, true);
        }

        if (m_currentTrail < m_maxTrails) {
            startTrial();
        } else {
            endBlock();
        }
    }

    public void closeLogger() {
        if (m_logger != null) {
            m_logger.close();
        }
    }

    public void logAndSendReceiveMessageToServer(String message){
        JSONObject msg = new JSONObject();

        try {
            JSONObject jsonObject = new JSONObject(message);
            //String ballId = jsonObject.getString("ballId");
            //int ballColor = jsonObject.getInt("ballColor");
            String receiverName = jsonObject.getString("receiverName");
            String senderName = jsonObject.getString("senderName");
            String senderId = jsonObject.getString("senderId");
            int blockId = jsonObject.getInt("blockId");
            int trialId = jsonObject.getInt("trialId");
            String transactionId = jsonObject.getString("transactionId");

            boolean isCorrect = receiverName.equalsIgnoreCase(m_name);
            msg.put("NFCLog", true);
            msg.put("senderId", senderId);
            msg.put("senderName", senderName);
            msg.put("receiverName", receiverName);
            msg.put("actualReceiverName", m_name);
            msg.put("blockId", blockId);
            msg.put("trialId", trialId);
            msg.put("transactionId", transactionId);
            msg.put("isCorrect", isCorrect);

            //<senderID> <senderName> <condition> <block#> <trial#> <receiverName> <actualReceiverName> <isCorrect> <transactionId> <timestamp>
            m_receiveLogger.write(senderId + "," + senderName + "," + getResources().getString(R.string.app_name) + "," + blockId + "," + trialId + "," + receiverName + "," + m_name + "," + isCorrect + "," + transactionId + "," + System.currentTimeMillis(), true);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        MainActivity mainActivity = (MainActivity)getContext();
        if (mainActivity != null) {
            mainActivity.addMessage(msg.toString());
        }
    }
    /**
     * experiment end
     */
}
