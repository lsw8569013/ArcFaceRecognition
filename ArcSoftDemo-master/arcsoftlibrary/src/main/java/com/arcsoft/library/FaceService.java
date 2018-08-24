package com.arcsoft.library;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.library.database.module.Face;

import com.arcsoft.library.module.ArcsoftFace;
import com.arcsoft.library.module.FaceData;
import com.arcsoft.library.module.FaceResponse;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class FaceService extends Service {

    //sdk 控制器
    private FaceManager manager;

    // 阈值 默认为0.7
    private float thresholdValue = 0.62f;

    private LocalBind local = new LocalBind();




    public class LocalBind extends Binder {
        public FaceService getService() {
            return FaceService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return local;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        manager.onDestory();
        EventBus.getDefault().unregister(this);
        return super.onUnbind(intent);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化
        manager = new FaceManager();
        manager.onCreate();
        Log.e("lsw","onCreate faceService");
        EventBus.getDefault().register(this);
    }

   //设置阈值
    public void setThresholdValue(float thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public boolean enroll(String path, String name) {
        FaceData data = manager.decodePath(path);
        if (data != null) {
            List<AFD_FSDKFace> list = manager.fdDetection(data);
            if (list != null) {
                EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.DETECTION, list));
                if (list.size() > 0) {
                    AFR_FSDKFace feature = manager.recognize(data, list.get(0).getRect(), list.get(0).getDegree());
                    if (feature != null) {
                        EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.RECOGNITION, feature.getFeatureData()));
                        ContentValues values = new ContentValues();
                        values.put(Face.NAME, name);
                        values.put(Face.FEATURE, Base64.encodeToString(feature.getFeatureData(), Base64.DEFAULT));
                        values.put(Face.PATH, path);
                        new Face(FaceService.this, values);
                        return true;
                    }
                }else{
                    EventBus.getDefault().post(new FaceResponse(134, FaceResponse.FaceType.DETECTION));
                }
            }else{
                EventBus.getDefault().post(new FaceResponse(135, FaceResponse.FaceType.DETECTION));
            }
        }else{
            EventBus.getDefault().post(new FaceResponse(136, FaceResponse.FaceType.DETECTION));
        }

        return false;
    }

    /**
     * 摄像头数据
     * @param data
     */
    public List<AFT_FSDKFace> detection(byte[] data, int width, int height, int orientation) {
        List<AFT_FSDKFace> list = null;
        if (data != null) {
             list = manager.ftDetection(data,width,height);
            if (list != null) {
                if(list.size() > 0) {
                    Log.e("lsw","识别到人脸~ 开始识别");
                }
                EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.DETECTION));
            }else{
                Log.e("lsw","");
            }
        }
        return list;
    }

    /**
     *
     * @param data
     * @param width
     * @param height
     * @param AFT_FSDKFace 识别出的人脸
     * @param orientation
     */
    public void cameraRecognize(byte[] data, int width, int height, AFT_FSDKFace afd_fsdkFace,int orientation) {

            AFR_FSDKFace feature = manager.recognize(data, width,  height, afd_fsdkFace.getRect(), afd_fsdkFace.getDegree());
            if (feature == null)
                return;
            EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.RECOGNITION, feature.getFeatureData()));
            int i = 0;
            while (true) {
                Face face = new Face(FaceService.this, i);
                if (face == null || TextUtils.isEmpty(face.getName())
                        || TextUtils.isEmpty(face.getPath())
                        || face.getFeature() == null
                        || face.getFeature().length <= 0) {
                    break;
                } else {
                    float  mScore = manager.match(feature, face);
                    Log.e("lsw","识别Score--- "+ mScore);
                    if(mScore > thresholdValue){

                        EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.MATCH, mScore, new ArcsoftFace(afd_fsdkFace), face.getName(), orientation));
                        break;
                    }
                }
                i++;
            }

    }

    public void cameraRecognize(FaceData data) {

//                for (AFT_FSDKFace afd_fsdkFace : list) {
//                    AFR_FSDKFace feature = manager.recognize(data, afd_fsdkFace.getRect(), afd_fsdkFace.getDegree());
//
//                    if (feature == null)
//                        return;
//                    EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.RECOGNITION, feature.getFeatureData()));
//                    int i = 0;
//                    while (true) {
//                        Face face = new Face(FaceService.this, i);
//                        if (face == null || TextUtils.isEmpty(face.getName())
//                                || TextUtils.isEmpty(face.getPath())
//                                || face.getFeature() == null
//                                || face.getFeature().length <= 0) {
//                            break;
//                        } else {
//                            float  mScore = manager.match(feature, face);
//                            Log.e("lsw","识别Score--- "+ mScore);
//                            if(mScore > thresholdValue){
//
//                                EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.MATCH, mScore, new ArcsoftFace(afd_fsdkFace), face.getName(), data.getOrientation()));
//                                break;
//                            }
//                        }
//                        i++;
//                    }
//                }

    }

    /*public void cameraRecognize2(FaceData data) {
        if (data != null) {
            List<AFT_FSDKFace> list = manager.ftDetection(data);
            if (list != null) {
                if(list.size() > 0) {
                    Log.e("lsw","识别到人脸~ 开始识别");
                }
                EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.DETECTION));
                for (AFT_FSDKFace afd_fsdkFace : list) {
                    byte[] feature = manager.recognize(data, afd_fsdkFace.getRect(), afd_fsdkFace.getDegree());
                    EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.RECOGNITION, feature));
                    if (feature == null)
                        return;
                    int i = 0;
                    while (true) {
                        Face face = new Face(FaceService.this, i);
                        if (face == null || TextUtils.isEmpty(face.getName())
                                || TextUtils.isEmpty(face.getPath())
                                || face.getFeature() == null
                                || face.getFeature().length <= 0) {
                            break;
                        } else {
                            float  mScore = manager.match(feature, face);
                            Log.e("lsw","识别Score--- "+ mScore);
                            if(mScore > thresholdValue){
                                EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.MATCH, mScore, afd_fsdkFace, face.getName(), data.getOrientation()));
                                break;
                            }
                        }
                        i++;
                    }
                }
            }else{
                Log.e("lsw","");
            }
        }
    }*/

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(FaceData data) {
        cameraRecognize(data);
    }

    ;
}
