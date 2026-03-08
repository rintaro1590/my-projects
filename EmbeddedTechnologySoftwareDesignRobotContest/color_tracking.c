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
/* データ取得用 */
int16_t r = 50;
int16_t lb,cb,rb;


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

    /* 白ピクセルカウントの取得 */
    FILE *fp;    
    fp = fopen("/home/etrobo/work/RasPike/sdk/workspace/data.txt", "r");
    if(fp == NULL) {
        printf("file not open!\n");
        return -1;
    }
    fscanf(fp, "%d-%d-%d", &r, &lb, &rb);
    printf("%d %d %d\n", r, lb, rb);
    fclose(fp);
    
    /* 画像の白線と白色のピルセル量により操作量計算 */
    steering_amount = (lb - rb) * 0.021;
    
    printf("st:%d\n",steering_amount);

    return steering_amount;
}

/* 走行モータ制御 */
static void motor_drive_control(int16_t steering_amount){

    int left_motor_power, right_motor_power; /*左右モータ設定パワー*/
    
    if(r){        
   	left_motor_power  = (int)(r + (steering_amount * LEFT_EDGE));
        right_motor_power = (int)(r - (steering_amount * LEFT_EDGE));
    }
    else{
        left_motor_power  = 0;
        right_motor_power = 0;
    }
    
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
