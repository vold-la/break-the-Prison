package noori.com.breakPrison;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView scoreText, hsText;
    private int score, hs, Count;
    private FrameLayout frameShrinker;
    private int fHeight, fWidth, startingWidth;
    private LinearLayout firstLayout;

    private ImageView prisoner, ray, hammer, dynamite;
    private Drawable prisonerRight, prisonerLeft;
    private int prisonerSize;

    private float prisonerX, prisonerY;
    private float rayX, rayY;
    private float hammerX, hammerY;
    private float dynamiteX, dynamiteY;

    private SharedPreferences sharedPreferences;
    private Timer timer;
    private Handler handler = new Handler();

    private boolean isStarted = false;
    private boolean isOnAction = false;
    private boolean dynamite_flag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        frameShrinker = findViewById(R.id.Frame);
        firstLayout = findViewById(R.id.firstLayout);
        prisoner = findViewById(R.id.prisoner);
        ray = findViewById(R.id.ray);
        hammer = findViewById(R.id.hammer);
        dynamite = findViewById(R.id.dynamite);
        scoreText = findViewById(R.id.score);
        hsText = findViewById(R.id.hS);

        prisonerLeft = getResources().getDrawable(R.drawable.left);
        prisonerRight = getResources().getDrawable(R.drawable.right);

        sharedPreferences = getSharedPreferences("DATA", Context.MODE_PRIVATE);
        hs = sharedPreferences.getInt("HIGH_SCORE", 0);
        hsText.setText("High Score : " + hs);
    }

    public void dropItem() {
        Count += 20;
        hammerY += 12;

        float hammerMidX = hammerX + hammer.getWidth() / 2;
        float hammerMidy = hammerY + hammer.getHeight() / 2;

        if (checkCollision(hammerMidX, hammerMidy)) {
            hammerY = fHeight + 100;
            score += 10;
        }

        if (hammerY > fHeight) {
            hammerY = -100;
            hammerX = (float) Math.floor(Math.random() * (fWidth - hammer.getWidth()));
        }
        hammer.setX(hammerX);
        hammer.setY(hammerY);

        if (!dynamite_flag && Count % 10000 == 0) {
            dynamite_flag = true;
            dynamiteY = -20;
            dynamiteX = (float) Math.floor(Math.random() * (fWidth - dynamite.getWidth()));
        }

        if (dynamite_flag) {
            dynamiteY += 20;

            float dynamiteMidx = dynamiteX + dynamite.getWidth() / 2;
            float dynamiteMidY = dynamiteY + dynamite.getWidth() / 2;

            if (checkCollision(dynamiteMidx, dynamiteMidY)) {
                dynamiteY = fHeight + 30;
                score += 30;

                //expand the prison
                if (startingWidth > fWidth * 110 / 100) {
                    fWidth = fWidth * 110 / 100;
                    updateFrame(fWidth);
                }
            }

            if (dynamiteY > fHeight) dynamite_flag = false;
            dynamite.setX(dynamiteX);
            dynamite.setY(dynamiteY);
        }

        rayY += 18;

        float rayMidX = rayX + ray.getWidth() / 2;
        float rayMidY = rayY + ray.getHeight() / 2;

        if (checkCollision(rayMidX, rayMidY)) {
            rayY = fHeight + 100;
          //shrink the prison
            fWidth = fWidth * 80 / 100;
            updateFrame(fWidth);
      //      Add sound
            if (fWidth <= prisonerSize) {
                gameOver();
            }

        }

        if (rayY > fHeight) {
            rayY = -100;
            rayX = (float) Math.floor(Math.random() * (fWidth - ray.getWidth()));
        }
        ray.setX(rayX);
        ray.setY(rayY);
        //on pressing
        if (isOnAction) {
           prisonerX += 14;
            prisoner.setImageDrawable(prisonerRight);
        } else {
            // on releasing
            prisonerX -= 14;
            prisoner.setImageDrawable(prisonerLeft);
        }
        if (prisonerX < 0) {
            prisonerX = 0;
            prisoner.setImageDrawable(prisonerRight);
        }
        if (fWidth - prisonerSize < prisonerX) {
            prisonerX = fWidth - prisonerSize;
            prisoner.setImageDrawable(prisonerLeft);
        }
        prisoner.setX(prisonerX);
        scoreText.setText("Score : " + score);

    }

    public boolean checkCollision(float x, float y) {
        if (prisonerX <= x && x <= prisonerX + prisonerSize &&
                prisonerY <= y && y <= fHeight) {
            return true;
        }
        return false;
    }

    public void updateFrame(int frameWidth) {
        ViewGroup.LayoutParams params = frameShrinker.getLayoutParams();
        params.width = frameWidth;
        frameShrinker.setLayoutParams(params);
    }

    public void gameOver() {
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
        ray.setVisibility(View.INVISIBLE);
        hammer.setVisibility(View.INVISIBLE);
        dynamite.setVisibility(View.INVISIBLE);

        if (score > hs) {
            hs = score;
            hsText.setText("High Score : " + hs);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("HIGH_SCORE", hs);
            editor.commit();
        }
    }

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

    public void start(View view) {
        isStarted = true;
        firstLayout.setVisibility(View.INVISIBLE);
        if (fHeight == 0) {
            fHeight = frameShrinker.getHeight();
            fWidth = frameShrinker.getWidth();
            startingWidth = fWidth;

            prisonerSize = prisoner.getHeight();
            prisonerX = prisoner.getX();
            prisonerY = prisoner.getY();
        }
        fWidth = startingWidth;
        prisoner.setX(0.0f);
        ray.setY(3000.0f);
        hammer.setY(3000.0f);
        dynamite.setY(3000.0f);

        rayY = ray.getY();
        hammerY = hammer.getY();
        dynamiteY = dynamite.getY();

        prisoner.setVisibility(View.VISIBLE);
        ray.setVisibility(View.VISIBLE);
        hammer.setVisibility(View.VISIBLE);
        dynamite.setVisibility(View.VISIBLE);

        Count = 0;
        score = 0;
        scoreText.setText("Score : 0");
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

}
