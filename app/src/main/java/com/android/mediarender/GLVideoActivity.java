package com.android.mediarender;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import static com.android.mediarender.ShaderUtil.createProgram;

public class GLVideoActivity extends FragmentActivity implements View.OnClickListener{
    private final String TAG = "GLVideoActivity";

    private final String TestUrl = "http://devimages.apple.com/iphone/samples/bipbop/gear4/prog_index.m3u8";
    private MediaPlayer mPlayer;
    private GLSurfaceView mGLSurfaceVeiw;
    private GLRender mGLRender;

    private Button mStartBtn;
    private Button mPauseBtn;
    private Button mResumeBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gl_activity);

        initView();
    }

    void initView() {
        mStartBtn = findViewById(R.id.button_start);
        mStartBtn.setOnClickListener(this);

        mPauseBtn = findViewById(R.id.button_pause);
        mPauseBtn.setOnClickListener(this);

        mResumeBtn = findViewById(R.id.button_resume);
        mResumeBtn.setOnClickListener(this);

        //glsurface
        mGLSurfaceVeiw = findViewById(R.id.glsurface);
        mGLSurfaceVeiw.setEGLContextClientVersion(3);

        mGLRender = new GLRender();
        mGLSurfaceVeiw.setRenderer(mGLRender);

        mGLSurfaceVeiw.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceCreated: ");
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged: ");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed: ");
                mGLRender.release();
            }
        });
    }

    void startPlayer(String url) {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(url);
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    Log.d(TAG, "onPrepared: ");
                    mediaPlayer.start();
                }
            });

            mPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {

                @Override
                public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
                    Log.d(TAG, "onVideoSizeChanged: ");
                    mGLRender.setVideoSize(i, i1);
                }
            });

//            mPlayer.setDisplay(mSurfaceHolder);
            mPlayer.setSurface(mGLRender.getSurface());
            mPlayer.prepareAsync();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_start: {
                startPlayer(TestUrl);
                break;
            }
            case R.id.button_pause: {
                if (mPlayer != null) {
                    mPlayer.pause();
                }
                break;
            }
            case R.id.button_resume: {
                if (mPlayer != null) {
                    mPlayer.start();
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    private class GLRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
        protected static final String DEFAULT_VERTEX_SHADER =
                "attribute vec4 aPosition;\n" + //顶点位置
                "attribute vec4 aTexCoord;\n" + //S T 纹理坐标
                "varying vec2 vTexCoord;\n" +
                "uniform mat4 uMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "void main() {\n" +
                    "vTexCoord = (uSTMatrix * aTexCoord).xy;\n" +
                    "gl_Position = uMatrix*aPosition;\n" +
                "}\n";

        protected static final String DEFAULT_FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTexCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                    "gl_FragColor = texture2D(sTexture, vTexCoord);\n" +
                "}\n";

        int programId;
        int aPositionLocation;
        int uMatrixLocation;
        int uSTMMatrixHandle;
        int uTextureSamplerLocation;
        int aTextureCoordLocation;
        int textureId;

        private int screenWidth, screenHeight;

        SurfaceTexture preViewSurface;
        Surface preSurface;
        boolean updateSurface;

        private FloatBuffer vertexBuffer;
        private final float[] vertexData = {
                1f, -1f, 0f,
                -1f, -1f, 0f,
                1f, 1f, 0f,
                -1f, 1f, 0f
        };
        private final float[] textureVertexData = {
                1f, 0f,
                0f, 0f,
                1f, 1f,
                0f, 1f
        };
        private FloatBuffer textureVertexBuffer;

//        private final float[] projectionMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private float[] mMMatrix = new float[16];
        private float[] mVMatrix = new float[16];
        private float[] mPMatrix = new float[16];
        private float[] mMVPMatrix = new float[16];

        GLRender() {

            synchronized (this) {
                updateSurface = false;
            }
            vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(vertexData);
            vertexBuffer.position(0);

            textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(textureVertexData);
            textureVertexBuffer.position(0);
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            Log.d(TAG, "onSurfaceCreated: ");
            gl10.glClearColor(0.5f, 0.0f, 0.0f, 1.0f);

            programId = createProgram(DEFAULT_VERTEX_SHADER, DEFAULT_FRAGMENT_SHADER);
            aPositionLocation = GLES30.glGetAttribLocation(programId, "aPosition");
            uMatrixLocation = GLES30.glGetUniformLocation(programId, "uMatrix");
            uSTMMatrixHandle = GLES30.glGetUniformLocation(programId, "uSTMatrix");
            uTextureSamplerLocation = GLES30.glGetUniformLocation(programId, "sTexture");
            aTextureCoordLocation = GLES30.glGetAttribLocation(programId, "aTexCoord");

            final int[] args = new int[1];
            GLES30.glGenTextures(1, args, 0);
            textureId = args[0];
            // 扩展纹理的作用就是实现YUV格式到RGB的自动转化
//            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
//            ShaderUtil.checkGlError("glBindTexture mTextureID");
//            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER,
//                    GLES30.GL_NEAREST);
//            GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER,
//                    GLES30.GL_LINEAR);

            preViewSurface = new SurfaceTexture(textureId);
            preViewSurface.setOnFrameAvailableListener(this);
            preSurface = new Surface(preViewSurface);

            Log.d(TAG, "onSurfaceCreated: end");

        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int i, int i1) {
            Log.d(TAG, "onSurfaceChanged: i:" + i + ",i1:" + i1);
            screenWidth = i;
            screenHeight = i1;
        }

        int rotateDegree = 0;
        int timecnt = 0;
        @Override
        public void onDrawFrame(GL10 gl10) {
//            Log.d(TAG, "onDrawFrame: ");
            GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
            synchronized (this) {
                if (updateSurface) {
                    preViewSurface.updateTexImage();//获取新数据
                    preViewSurface.getTransformMatrix(mSTMatrix);//让新的纹理和纹理坐标系能够正确的对应
                    updateSurface = false;
                }
            }

            GLES30.glUseProgram(programId);


            Matrix.setIdentityM(mMMatrix, 0);
            Matrix.setIdentityM(mVMatrix, 0);
            Matrix.setIdentityM(mPMatrix, 0);

            // model Matrix
            float scaleValue = (float) (1.0 + 0.3 * Math.sin(Math.toRadians(timecnt++)));
            Matrix.scaleM(mMMatrix, 0, scaleValue, scaleValue, scaleValue);
            rotateDegree++;
            rotateDegree = rotateDegree % 360;
            Matrix.rotateM(mMMatrix, 0, rotateDegree, 0, 1, 0);

            // view Matrix
            Matrix.setLookAtM(mVMatrix, 0,
                    0.0f, 3.0f, -5.0f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f);

            // project Martix
            Matrix.orthoM(mPMatrix, 0, -2f, 2f, -2f, 2f, -10f, 10f);
            Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mPMatrix, 0, mMVPMatrix, 0);

            GLES30.glUniformMatrix4fv(uMatrixLocation, 1, false, mMVPMatrix, 0);
            GLES30.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);

            vertexBuffer.position(0);
            GLES30.glEnableVertexAttribArray(aPositionLocation);
            GLES30.glVertexAttribPointer(aPositionLocation, 3, GLES30.GL_FLOAT, false,
                    12, vertexBuffer);

            textureVertexBuffer.position(0);
            GLES30.glEnableVertexAttribArray(aTextureCoordLocation);
            GLES30.glVertexAttribPointer(aTextureCoordLocation, 2, GLES30.GL_FLOAT, false, 8, textureVertexBuffer);

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

            GLES30.glUniform1i(uTextureSamplerLocation, 0);
//            GLES30.glViewport(0, 0, screenWidth / 2, screenHeight / 2);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        }

        @Override
        synchronized public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//            Log.d(TAG, "onFrameAvailable: ");
            updateSurface = true;
        }

        Surface getSurface() {
            Log.d(TAG, "getSurface: " + preSurface);
            return preSurface;
        }

        void setVideoSize(int width, int height) {
            float screenRatio = (float) screenWidth / screenHeight;
            float videoRatio = (float) width / height;
//            if (videoRatio > screenRatio) {
//                Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
////                Matrix.orthoM(projectionMatrix, 0, -2f, 2f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
//            } else
//                Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        }

        void release() {
            Log.d(TAG, "release: ");
            preSurface.release();
        }

    }
}