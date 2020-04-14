package noori.com.breakPrison;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView scoreText, hsText;
    private int score, hs, time, targeted_spotlight_pos, move_spotlight;
    private FrameLayout prison_visible_frame;
    private int fHeight, fWidth, startingWidth ;
    private LinearLayout firstLayout;
    private Button sound;

    private ImageView prisoner, cops_light, hammer, spade , prison , spotlight;
    private Drawable prisonerRight, prisonerLeft;
    private int prisonerSize , currentPrison_image;

    private float prisonerX, prisonerY;
    private float cops_lightX, cops_lightY;
    private float hammerX, hammerY;
    private float spadeX, spadeY;
    private float spotlightX ;
    private float fromDegree , toDegree ,temp;

    private SharedPreferences sharedPreferences;
    private Timer timer;
    private Handler handler = new Handler();

    private boolean isStarted = false;
    private boolean isOnAction = false;
    private boolean spade_flag = false ;
    private boolean isGotNewSpotlight , isMuted , isPaused;
    private String HS,SC;

    public MediaPlayer wall , item_collected , cast_spotlight;

    int [] prisonImage ={R.drawable.prison1, R.drawable.prison2, R.drawable.prison3, R.drawable.prison4, R.drawable.prison5, R.drawable.prison6, R.drawable.prison7, R.drawable.prison8, R.drawable.prison9};

    public Animation mAnimation;

    //ignore deprecated of getDrawable since my theme is null so no need to use different code for below 21 and after 21 API
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prison_visible_frame = findViewById(R.id.Frame);
        firstLayout = findViewById(R.id.firstLayout);
        prisoner = findViewById(R.id.prisoner);
        prison = findViewById(R.id.prison);
        cops_light = findViewById(R.id.cop);
        hammer = findViewById(R.id.hammer);
        spade = findViewById(R.id.spade);
        scoreText = findViewById(R.id.score);
        hsText = findViewById(R.id.hS);
        spotlight = findViewById(R.id.spotlight);
        sound = findViewById(R.id.sound);

        prisonerLeft = getResources().getDrawable(R.drawable.left);
        prisonerRight = getResources().getDrawable(R.drawable.right);

        sharedPreferences = getSharedPreferences("DATA", Context.MODE_PRIVATE);
        hs = sharedPreferences.getInt("HIGH_SCORE", 0);
        HS = getString(R.string.hs) + String.valueOf(hs);
        hsText.setText(HS);

        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isMuted = !isMuted;

                if(isMuted)
                    sound.setText(R.string.sound);
                else
                    sound.setText(R.string.mute);
            }
        });

        // creating three different player to avoid mismatch
        item_collected = MediaPlayer.create(MainActivity.this,R.raw.item_touch);
        cast_spotlight = MediaPlayer.create(MainActivity.this,R.raw.casting_light);
        wall = MediaPlayer.create(MainActivity.this,R.raw.sliding_wall);

    }


    public void dropItem() {

        /* move the spotlight to targeted position or set the to and fro movement of spotlight
          currentPrison < 5 indicates to cast the spotlight on prisoner only when the frame is big enough*/
        if(spotlight.getX() == targeted_spotlight_pos && !isGotNewSpotlight && currentPrison_image < 5)
        {
            // adding this to avoid making extra flag , we want to start only during first insertion into the block
            if(!mAnimation.hasEnded())
                cast_spotlight.start();

            mAnimation.cancel();

            //cast the light on the prisoner
            ViewGroup.LayoutParams params = spotlight.getLayoutParams();
            params.height = prison_visible_frame.getHeight();
            spotlight.setLayoutParams(params);

            //if prisoner get caught inside the spotlight , shrink the prison.
            spotlightX = spotlight.getX() + spotlight.getWidth() / 2.0f;
            if (prisonerX <= spotlightX && spotlightX <= prisonerX + prisonerSize){
                fWidth = fWidth * 80 / 100;
                if(currentPrison_image < 8)
                    currentPrison_image++;

                prison.setImageDrawable(getResources().getDrawable(prisonImage[currentPrison_image]));
                updateFrame(fWidth);
                if (fWidth <= prisonerSize) {
                    gameOver();
                }
            }
        }
        //set to and fro movement
        else {

            //cast the spotlight away from prisoner
            ViewGroup.LayoutParams params = spotlight.getLayoutParams();
            params.height = prison_visible_frame.getHeight() * 9 / 10;
            spotlight.setLayoutParams(params);

            if (targeted_spotlight_pos > spotlight.getX())
                move_spotlight += 1;
            else
                move_spotlight -= 1;

            //since animation.setRepeat() doesn't works.
            if (time % 500 == 0) {

            if(fromDegree == 0)
                toDegree = -1.0f * toDegree;

                mAnimation = new RotateAnimation(fromDegree, toDegree, 0.0f, 0.0f);
                mAnimation.setDuration(500);
                mAnimation.setInterpolator(new LinearInterpolator());
                spotlight.startAnimation(mAnimation);

                //swap the degrees
                 temp = fromDegree;
                 fromDegree = toDegree;
                 toDegree = temp ;
            }
            isGotNewSpotlight = false;
        }


        time += 20;
        hammerY += 12;

        // dropping hammer
        float hammer_midX = hammerX + hammer.getWidth() / 2.0f;
        float hammer_midY = hammerY + hammer.getHeight() / 2.0f;

        if (checkCollision(hammer_midX, hammer_midY)) {
            hammerY = fHeight + 100;
            score += 10;
        }

        // if reached bottom of screen
        if (hammerY > fHeight) {
            hammerY = -100;
            hammerX = (float) Math.floor(Math.random() * (fWidth - hammer.getWidth()));
        }

        hammer.setX(hammerX);
        hammer.setY(hammerY);
        //end of hammer portion

        // dropping of spade , drops in every 10 sec
        if (!spade_flag && time % 10000 == 0) {
            spade_flag = true;
            spadeY = -20;
            spadeX = (float) Math.floor(Math.random() * (fWidth - spade.getWidth()));

            targeted_spotlight_pos = (int) Math.floor(Math.random() * (fWidth - spade.getWidth()));
            isGotNewSpotlight = true;
        }

        if (spade_flag)
        {
            spadeY += 20;

            float spade_midX = spadeX + spade.getWidth() / 2.0f;
            float spade_midY = spadeY + spade.getWidth() / 2.0f;

            if (checkCollision(spade_midX, spade_midY)) {
                spadeY = fHeight + 30;
                score += 30;

                //expand the prison
                if (startingWidth > fWidth * 120 / 100) {
                    fWidth = fWidth * 120 / 100;

                    //cut prison image
                    if(currentPrison_image >0)
                        currentPrison_image--;

                    prison.setImageDrawable(getResources().getDrawable(prisonImage[currentPrison_image]));
                    updateFrame(fWidth);
                }
            }

            if (spadeY > fHeight) spade_flag = false;
            spade.setX(spadeX);
            spade.setY(spadeY);
        }
        //end of spade portion

        //dropping of cops_light
        cops_lightY += 18;

        float cops_light_midX = cops_lightX + cops_light.getWidth() / 2.0f;
        float cops_light_midY = cops_lightY + cops_light.getHeight() / 2.0f;

        if (checkCollision(cops_light_midX, cops_light_midY))
        {
            cops_lightY = fHeight + 100;

          //shrink the prison
            fWidth = fWidth * 80 / 100;

            if(currentPrison_image < 8)
                currentPrison_image++;

            prison.setImageDrawable(getResources().getDrawable(prisonImage[currentPrison_image]));
            updateFrame(fWidth);
            if (fWidth <= prisonerSize) {
                gameOver();
            }

        }

        if (cops_lightY > fHeight) {
            cops_lightY = -100;
            cops_lightX = (float) Math.floor(Math.random() * (fWidth - cops_light.getWidth()));
        }
        cops_light.setX(cops_lightX);
        cops_light.setY(cops_lightY);
        //end of cops_light portion


        //on pressing
        if (isOnAction) {
           prisonerX += 14;
            prisoner.setImageDrawable(prisonerRight);
        }
        // on releasing
        else {
            prisonerX -= 14;
            prisoner.setImageDrawable(prisonerLeft);
        }

        //switching prisoner image
        if (prisonerX < 0) {
            prisonerX = 0;
            prisoner.setImageDrawable(prisonerRight);
        }
        if (fWidth - prisonerSize < prisonerX) {
            prisonerX = fWidth - prisonerSize;
            prisoner.setImageDrawable(prisonerLeft);
        }

        prisoner.setX(prisonerX);
        spotlight.setX(move_spotlight);
        SC=getString(R.string.score) + String.valueOf(score) ;
        scoreText.setText(SC);

    }

    public boolean checkCollision(float x, float y) {
        if (prisonerX <= x && x <= prisonerX + prisonerSize && prisonerY <= y && y <= fHeight) {
            if (!isMuted)
                item_collected.start();

            return true;
        }
        return false;
    }

    public void updateFrame(int frameWidth) {
        if (!isMuted)
            wall.start();

        ViewGroup.LayoutParams params = prison_visible_frame.getLayoutParams();
        params.width = frameWidth;
        prison_visible_frame.setLayoutParams(params);
       }

    public void gameOver() {

        currentPrison_image = 0;
        prison.setImageDrawable(getResources().getDrawable(R.drawable.prison));

        timer.cancel();
        timer = null;
        isStarted = false;

        // Wait for 1 sec before showing start.
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        updateFrame(startingWidth);

        firstLayout.setVisibility(View.VISIBLE);
        prisoner.setVisibility(View.INVISIBLE);
        cops_light.setVisibility(View.INVISIBLE);
        hammer.setVisibility(View.INVISIBLE);
        spade.setVisibility(View.INVISIBLE);

        if (score > hs)
        {
            HS = getString(R.string.hs) + String.valueOf(score);
            hsText.setText(HS);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("HIGH_SCORE", score);
            editor.apply();
        }
    }

    //triggers on user's touch
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isStarted) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isOnAction = true;

            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                isOnAction = false;

            }
        }
        return true;
    }

    //start on click of Start Button
    public void start(View view) {
        isPaused = false ;
        isStarted = true;
        firstLayout.setVisibility(View.INVISIBLE);

        if (fHeight == 0) {
            fHeight = prison_visible_frame.getHeight();
            fWidth = prison_visible_frame.getWidth();
            startingWidth = fWidth;

            prisonerSize = prisoner.getHeight();
            prisonerX = prisoner.getX();
            prisonerY = prisoner.getY();
        }

        fWidth = startingWidth;
        prisoner.setX(0.0f);
        cops_light.setY(3000.0f);
        hammer.setY(3000.0f);
        spade.setY(3000.0f);

        cops_lightY = cops_light.getY();
        hammerY = hammer.getY();
        spadeY = spade.getY();

        prisoner.setVisibility(View.VISIBLE);
        cops_light.setVisibility(View.VISIBLE);
        hammer.setVisibility(View.VISIBLE);
        spade.setVisibility(View.VISIBLE);

        time = 0;
        score = 0;
        fromDegree =0.0f;
        toDegree = 5.0f;
        targeted_spotlight_pos = 420;
        scoreText.setText(R.string.score);


        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isStarted) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            dropItem();
                        }
                    });
                }
            }
        }, 0, 20);
    }


    public void Exit(View view) {
            finish();
    }


    public void help(View view) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(R.string.how_to_play)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }
    //cancel the timer and saves the state of variables
    @Override
    protected void onPause() {
        if (timer != null && isStarted) {
            timer.cancel();
            timer = null;
        }

        isPaused = true;
        super.onPause();
    }

    //create new timer and starts the looping of gloves ;
    // since every field was saved therefore every states start from where it was left.
    @Override
    protected void onResume() {
         if(isPaused && timer == null && isStarted)
        {
            isPaused = false;
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            dropItem();
                        }
                    });

                }
            }, 0, 20);
        }
        super.onResume();
    }
}
