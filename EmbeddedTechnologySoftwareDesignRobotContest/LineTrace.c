#include "app.h"
#include "LineTracer.h" /* 設定値ヘッダー */
#include <stdio.h>

/* 関数プロトタイプ宣言 */
static int16_t steering_amount_calculation(void);
static void motor_drive_control(int16_t);

/* I制御用:過去の偏差の累積値 */
float32_t integ_val;
/* D制御用:現在の変化率 */
float32_t diff_val;
/* RGB値の現在値 */
int16_t old_rgb = (WHITE_BRIGHTNESS + BLACK_BRIGHTNESS) / 2;

/* ライントレースタスク(10msec周期で関数コールされる) */
void tracer_task(intptr_t unused) {
    /* ステアリング操舵量の計算 */
    int16_t steering_amount; 
    
    /* ステアリング操舵量の計算 */
    steering_amount = steering_amount_calculation();

    /* 走行モータ制御 */
    motor_drive_control(steering_amount);

    /* タスク終了 */
    ext_tsk();
}

/* ステアリング操舵量の計算 */
static int16_t steering_amount_calculation(void){

    uint16_t  target_brightness; /* 目標輝度値 */
    float32_t diff_brightness;   /* 目標輝度との差分値 */
    int16_t   steering_amount;   /* ステアリング操舵量 */
    rgb_raw_t rgb_val;           /* カラーセンサ取得値 */
    int16_t   new_rgb;           /* RGB値取得値 */

    /* 目標輝度値の計算 */
    target_brightness = (WHITE_BRIGHTNESS + BLACK_BRIGHTNESS) / 2;

    /* カラーセンサ値の取得 */
    ev3_color_sensor_get_rgb_raw(color_sensor, &rgb_val);
    new_rgb=rgb_val.g;

    /* 目標輝度値とカラーセンサ値の差分を計算 */
    diff_brightness = (float32_t)(target_brightness - new_rgb);
    printf("g:%d\n",rgb_val.g);
    
    /* D制御 */
    diff_val = (float32_t)(old_rgb - new_rgb)/0.01;
    
    /* I制御 */
    integ_val += (float32_t)(old_rgb + new_rgb)*0.01/2;
    
    /* RGB値更新 */
    old_rgb = new_rgb;
    
    /* ステアリング操舵量を計算 */
    steering_amount = (int16_t)(diff_brightness * STEERING_COEF) + (integ_val * INTEG_COEF) + (diff_val * DIFF_COEF);
    printf("st:%d\n",steering_amount);

    return steering_amount;
}

/* 走行モータ制御 */
static void motor_drive_control(int16_t steering_amount){
    /*左右モータ設定パワー */
    int left_motor_power, right_motor_power; 

    /* 左右モータ駆動パワーの計算(走行エッジを右にする場合はRIGHT_EDGEに書き換えること) */
    //left_motor_power  = (int)(BASE_SPEED + (steering_amount * RIGHT_EDGE));
    //right_motor_power = (int)(BASE_SPEED - (steering_amount * RIGHT_EDGE));
    
    left_motor_power  = (int)(BASE_SPEED + (steering_amount * LEFT_EDGE));
    right_motor_power = (int)(BASE_SPEED - (steering_amount * LEFT_EDGE));
    
    /* 限界値補正(エラーの非表示) */ 
    if(left_motor_power > 100)
    {
        left_motor_power = 100;
    }
    if(right_motor_power > 100)
    {
        right_motor_power = 100;
    }
    
    if(left_motor_power < -100)
    {
        left_motor_power = -100;
    }
    if(right_motor_power < -100)
    {
        right_motor_power = -100;
    }
    
    printf("lm:%d\n",left_motor_power);
    printf("rm:%d\n",right_motor_power);

    
    /* 左右モータ駆動パワーの設定 */
    ev3_motor_set_power(left_motor, left_motor_power);
    ev3_motor_set_power(right_motor, right_motor_power);
    ev3_motor_set_power(arm_motor,20);

    return;
}
