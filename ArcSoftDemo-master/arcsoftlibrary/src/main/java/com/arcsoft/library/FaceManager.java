package com.arcsoft.library;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.library.database.module.Face;
import com.arcsoft.library.module.FaceData;
import com.arcsoft.library.module.FaceResponse;
import com.arcsoft.library.utils.BitmapUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by zsq on 2017/7/25.
 */

public class FaceManager {
    private final static String APPID = "Ed1coKcEk6uFuvAeTxm8gbLTRZspPRLYM3EAJekTnCYf";
    private final static String APD_SDK = "CoSRLeXsFqmcsWLJwGxn6t9KU1jvHUifPSv9to14E3gH";
    private final static String APR_SDK = "CoSRLeXsFqmcsWLJwGxn6t9p7cncxcNkTEn1afhHNDUw";
    private final static String APT_SDK = "CoSRLeXsFqmcsWLJwGxn6t9CJcUpA2iYn8CDSsj7mBr5";

    private AFD_FSDKEngine afdFsdkEngine;
    private AFR_FSDKEngine afrFsdkEngine;
    private AFT_FSDKEngine aftFsdkEngine;

    private AFD_FSDKError afdFsdkError;
    private AFR_FSDKError afrFsdkError;
    private AFT_FSDKError aftFsdkError;

    private boolean isInit = false;

    public FaceManager() {
        afdFsdkEngine = new AFD_FSDKEngine();
        afrFsdkEngine = new AFR_FSDKEngine();
        aftFsdkEngine = new AFT_FSDKEngine();


    }

    public  Object getSDKVersion() {
        AFD_FSDKVersion version = new AFD_FSDKVersion();
        afdFsdkEngine.AFD_FSDK_GetVersion(version);
        Log.e("lsw","---AFD  version "+version.toString());

        AFT_FSDKVersion version1 = new AFT_FSDKVersion();
        aftFsdkEngine.AFT_FSDK_GetVersion(version1);
        Log.e("lsw","---AFT  version "+version1.toString());

        AFR_FSDKVersion version2 = new AFR_FSDKVersion();
        afrFsdkEngine.AFR_FSDK_GetVersion(version2);
        Log.e("lsw","---AFR  version "+version1.toString());

        return version.toString();
    }


    public void onCreate() {
        init();
        getSDKVersion();
    }


    public void onDestory() {
        afdFsdkEngine.AFD_FSDK_UninitialFaceEngine();
        afrFsdkEngine.AFR_FSDK_UninitialEngine();
        aftFsdkEngine.AFT_FSDK_UninitialFaceEngine();
        afdFsdkEngine = null;
        afrFsdkEngine = null;
        aftFsdkEngine = null;
        afrFsdkError = null;
        afdFsdkError = null;
        aftFsdkError = null;
    }


    public boolean init() {
        if (isInit) {
            EventBus.getDefault().post(new FaceResponse(0, FaceResponse.FaceType.INIT));
            return true;
        }
        afdFsdkError = afdFsdkEngine.AFD_FSDK_InitialFaceEngine(APPID, APD_SDK, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
        int code = afdFsdkError.getCode();
        if (afdFsdkError.getCode() == 0) {
            afrFsdkError = afrFsdkEngine.AFR_FSDK_InitialEngine(APPID, APR_SDK);
            code = afrFsdkError.getCode();
            if (afrFsdkError.getCode() == 0) {
                aftFsdkError = aftFsdkEngine.AFT_FSDK_InitialFaceEngine(APPID, APT_SDK, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
                if (aftFsdkError.getCode() == 0) {
                    isInit = true;
                    EventBus.getDefault().post(new FaceResponse(code, FaceResponse.FaceType.INIT));
                    return true;
                }else{
                    Log.e("lsw","aftFsdkError");
                }
            }else{
                Log.e("lsw","afrFsdkError");
            }
        }else{
            Log.e("lsw","afdFsdkError");
        }



        EventBus.getDefault().post(new FaceResponse(code, FaceResponse.FaceType.INIT));
        return false;
    }

    public FaceData decodePath(String path) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            byte[] nv21 = BitmapUtils.getNV21(width, height, bitmap);
            return new FaceData(nv21, width, height, 0);
        } catch (Exception e) {
            e.printStackTrace();
            EventBus.getDefault().post(new FaceResponse(0XA001, FaceResponse.FaceType.DETECTION));
        }
        return null;
    }


    public List<AFD_FSDKFace> fdDetection(FaceData data) {
        int width = data.getWidth();
        int height = data.getHeight();
        byte[] nv21 = data.getNv21();
        List<AFD_FSDKFace> list = new ArrayList<>();
        afdFsdkError = afdFsdkEngine.AFD_FSDK_StillImageFaceDetection(nv21, width, height, AFD_FSDKEngine.CP_PAF_NV21, list);
        if (afdFsdkError.getCode() == 0) {
            return list;
        }
        EventBus.getDefault().post(new FaceResponse(afdFsdkError.getCode(), FaceResponse.FaceType.DETECTION));
        return null;
    }

    public List<AFT_FSDKFace> ftDetection(byte[] data, int width, int height) {
        List<AFT_FSDKFace> list = new ArrayList<>();
        aftFsdkError = aftFsdkEngine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, list);
        if (aftFsdkError.getCode() == 0) {
            return list;
        }
        EventBus.getDefault().post(new FaceResponse(aftFsdkError.getCode(), FaceResponse.FaceType.DETECTION));
        return null;
    }

    public List<AFT_FSDKFace> ftDetection(FaceData data){
        int width = data.getWidth();
        int height = data.getHeight();
        byte[] nv21 = data.getNv21();
        return null;
    }


    public AFR_FSDKFace recognize(byte[] nv21, int width, int height, Rect rect, int degree) {
        AFR_FSDKFace afr_fsdkFace = new AFR_FSDKFace();
        byte[] face = new byte[22020];
        afr_fsdkFace.setFeatureData(face);

        afrFsdkError = afrFsdkEngine.AFR_FSDK_ExtractFRFeature(nv21, width, height,
                AFR_FSDKEngine.CP_PAF_NV21, rect, degree, afr_fsdkFace);
        if (afrFsdkError.getCode() == 0) {
            return afr_fsdkFace;
        }
        Log.e("lsw","afrFsdkError.getCode() -- "+ afrFsdkError.getCode());
        EventBus.getDefault().post(new FaceResponse(afrFsdkError.getCode(), FaceResponse.FaceType.RECOGNITION));
        return null;
    }


    public AFR_FSDKFace recognize(FaceData data, Rect rect, int degree) {
        AFR_FSDKFace afr_fsdkFace = new AFR_FSDKFace();
        byte[] face = new byte[22020];
        afr_fsdkFace.setFeatureData(face);
        int width = data.getWidth();
        int height = data.getHeight();
        byte[] nv21 = data.getNv21();
        afrFsdkError = afrFsdkEngine.AFR_FSDK_ExtractFRFeature(nv21, width, height,
                AFR_FSDKEngine.CP_PAF_NV21, rect, degree, afr_fsdkFace);
        if (afrFsdkError.getCode() == 0) {
            return afr_fsdkFace;
        }
        Log.e("lsw","afrFsdkError.getCode() -- "+ afrFsdkError.getCode());
        EventBus.getDefault().post(new FaceResponse(afrFsdkError.getCode(), FaceResponse.FaceType.RECOGNITION));
        return null;
    }


    public float match(AFR_FSDKFace mface1, Face mface2) {
        AFR_FSDKMatching score = new AFR_FSDKMatching();
        AFR_FSDKFace afr_fsdkFace = new AFR_FSDKFace();
        afr_fsdkFace.setFeatureData(mface2.getFeature());
        afrFsdkError = afrFsdkEngine.AFR_FSDK_FacePairMatching(mface1, afr_fsdkFace, score);
        if (afrFsdkError.getCode() == 0) {
            return score.getScore();
        }
        EventBus.getDefault().post(new FaceResponse(afrFsdkError.getCode(), FaceResponse.FaceType.MATCH));
        return 0;
    }



}
