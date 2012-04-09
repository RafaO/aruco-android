package es.ava.aruco.android;

import org.opencv.android;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.SurfaceHolder;
import es.ava.aruco.BoardDetector;
import es.ava.aruco.CameraParameters;
import es.ava.aruco.MarkerDetector;
import es.ava.aruco.Utils;
import es.ava.aruco.exceptions.CPException;
import es.ava.aruco.exceptions.ExtParamException;

class View extends ViewBase {
	private Mat mFrame;
    protected CameraParameters mCamParam;
    protected float markerSizeMeters;
    protected Aruco3dActivity	mRenderer;
    protected MarkerDetector mDetector;
    protected BoardDetector mBDetector;
    
    public View(Context context, Aruco3dActivity renderer, CameraParameters cp, float markerSize) {
        super(context, renderer);
        
        mCamParam = new CameraParameters(cp);
        mDetector = new MarkerDetector();
        mBDetector = new BoardDetector();
        mRenderer = renderer;
        markerSizeMeters = markerSize;
    }

    @Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        super.surfaceChanged(_holder, format, width, height);

        synchronized (this) {
            // initialize Mats before usage
        	mFrame = new Mat();
        	
    		double[] proj_matrix = new double[16];
    		try {
    			Utils.myProjectionMatrix(mCamParam, new Size(width,height), proj_matrix, 0.05, 10);
//    			Utils.glGetProjectionMatrix(mCamParam, new Size(width,height),
//    					new Size(width, height), proj_matrix, 0.05, 10);
    		} catch (CPException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (ExtParamException e){
    			e.getMessage();
    		}
    		mRenderer.setProjMatrix(proj_matrix);
        }
    }
    
    @Override
    protected Bitmap processFrame(VideoCapture capture, SurfaceHolder holder, int width, int height) {
        capture.retrieve(mFrame, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
		
		mDetector.detect(mFrame, mDetectedMarkers, mCamParam.getCameraMatrix(), mCamParam.getDistCoeff(), markerSizeMeters,mFrame);
		mRenderer.onDetection(mFrame, mDetectedMarkers);
		
		if(mRenderer.lookForBoard == true){
			float prob=0f;
			try {
				prob = mBDetector.detect(mDetectedMarkers, mRenderer.mBC, mBoardDetected, mCamParam, markerSizeMeters);
			} catch (CvException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mRenderer.onBoardDetection(mFrame, mBoardDetected, prob);
		}
        Bitmap bmp = Bitmap.createBitmap(mFrame.cols(), mFrame.rows(), Bitmap.Config.ARGB_8888);

        boolean ret = android.MatToBitmap(mFrame,bmp);

        if (ret)
            return bmp;

        bmp.recycle();
        return null;
    }

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
        	if(mFrame != null)
        		mFrame.dispose();
        	mFrame = null;
        }
    }
}