from picamera2 import Picamera2
import cv2
import numpy as np
import time

#カメラ画素数（実環境に合わせて修正）
camera_H = 300 #幅
camera_W = 400 #高さ
B_center = 200

#二値化設定
th = 5 #閾値
i_max = 255 #閾値超えたら置き換える値

#青色検知関数
def detect_blue_color(img):
    hsv = cv2.cvtColor(img,cv2.COLOR_BGR2HSV_FULL)
    # 青色のHSVの値域
    hsv_min = np.array([140,127,0])
    hsv_max = np.array([190,255,255])

    # 青色領域のマスク（255：青色、0：青色以外）
    mask = cv2.inRange(hsv, hsv_min, hsv_max)
    
    masked_img = cv2.bitwise_and(img, img, mask=mask)

    return masked_img

#HSV画像のグレースケール化関数
def hsv2gray(img):
    
    (h,s,v) = cv2.split(img) #h,s,v に分割
    s[:] = 0 #彩度を0
    merge = cv2.merge((h, s, v))
    rgb = cv2.cvtColor(merge, cv2.COLOR_HSV2RGB) #hsv→rgb変換
    gray = cv2.cvtColor(rgb, cv2.COLOR_RGB2GRAY) #rgb→グレースケール化
    
    return gray

flag = True
# カメラ設定
pc2 = Picamera2()
pc2.configure(pc2.create_preview_configuration(main={"size": (camera_W,camera_H)}))
pc2.start()
time.sleep(1)

while flag:
	pc2.capture_file("test.jpg")
	img = cv2.imread("test.jpg")
	# 青色検出
	blue_frame = detect_blue_color(img) 
	gray_frame_blue = hsv2gray(blue_frame)
	ret, bi_frame_blue = cv2.threshold(gray_frame_blue, th, i_max, cv2.THRESH_BINARY)
	# 制御用のラインの表示
	cv2.line(bi_frame_blue,(B_center,0),(B_center,camera_H),(255,255,255))

	LB = bi_frame_blue[0:camera_H, 0:B_center] #左ブロックエリア
	RB = bi_frame_blue[0:camera_H, camera_W - B_center:camera_W] #右ブロックエリア
	Det_LB = cv2.countNonZero(LB) #左ブロックエリアの白ピクセルカウント
	Det_RB = cv2.countNonZero(RB) - 300 #右ブロックエリアの白ピクセルカウント(300はラインを含むため)
	
	f = open("data.txt","w")
	
	/* rを0で走行体を停止(rでスピード調節)
	if Det_LB <= 0 and Det_RB <= 0:
	    r = 0
	else:
	    r = 0
	
	if Det_LB >= 18000 or Det_RB >= 18000:
	    r = 0
	s = str(r)+'-'+str(Det_LB)+'-'+str(Det_RB)
	f.write(s)
	
	print(s)
	time.sleep(0.1)
	# 二値化画像を表示
	cv2.imshow('blue_white', bi_frame_blue)
	# 'q'で走行体停止
	if cv2.waitKey(16) & 0xFF == ord('q'):
		flag = False	
		s = str(0)+'-'+str(0)+'-'+str(0)
		f.write(s)

f.close()		
pc2.close()
cv2.destroyAllWindows()
