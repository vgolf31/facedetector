package com.mine.facedetector;

import java.io.IOException;

public interface OnFaceDetectedListener {

    void onFaceDetected(Boolean isDetected) throws IOException;

    void onMultipleFaceDetected();
}
