package com.tradplus.crosspro.ui;

import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.tradplus.ads.common.util.Audio;
import com.tradplus.ads.common.util.LogUtil;
import com.tradplus.ads.base.CommonUtil;
import com.tradplus.ads.base.network.response.CPAdResponse;
import com.tradplus.crosspro.manager.CPResourceManager;
import com.tradplus.crosspro.manager.resource.CPVideoUtil;
import com.tradplus.crosspro.network.base.CPError;
import com.tradplus.crosspro.network.base.CPErrorCode;
import com.tradplus.crosspro.ui.util.ViewUtil;

import java.io.FileDescriptor;
import java.io.FileInputStream;


public class PlayerView extends RelativeLayout implements TextureView.SurfaceTextureListener {

    public static final String TAG = PlayerView.class.getSimpleName();

    private MediaPlayer mMediaPlayer;
    private SurfaceTexture mSurfaceTexture;
    private TextureView mTextureView;
    private Surface mSurface;

    private FileInputStream mFileInputStream;
    private FileDescriptor mSourceFD;
    private String mSourcePath;

    private int mVideoWidth;
    private int mVideoHeight;

    private int mCurrentPosition = -1;//Current progress
    private int mDuration;
    private int mVideoProgress25;//progress==25
    private int mVideoProgress50;//progress==50
    private int mVideoProgress75;//progress==75
    private boolean mVideoPlay25;
    private boolean mVideoPlay50;
    private boolean mVideoPlay75;

    private boolean mFlag = false;//Loop read mark for playback progress
    private boolean mIsVideoStart = false;
    private boolean mIsVideoPlayCompletion = false;
    private boolean mIsMediaPlayerPrepared = false;

    private OnPlayerListener mListener;
    private Handler mMainHandler;


    private int mViewSizeDp = 29;//dp
    private int mViewMarginDp = 60;//dp
    private int mLeftMarginDp = 19;//dp
    private int mTopMarginDp = 30;//dp
    private int mViewSize;
    private int mViewMargin;
    private int mLeftMargin;
    private int mTopMargin;

    private int mMuteResId;
    private int mNoMuteResId;
    private int mCloseResId;
    private CountDownView mCountDownView;
    private ImageView mMuteBtn;
    private ImageView mCloseBtn;

    private final int mCountDownViewIndex = 1;
    private final int mMuteButtonIndex = 2;
    private final int mCloseButtonIndex = 3;
    private final int mAdChoiceIconIndex = 4;
    private final int mSkipIndex = 5;
    private CPAdResponse cpAdResponse;

    private boolean mIsMute;
    private long mShowCloseTime;
    private Thread mProgressThread;
    private ImageView mAdChoiceIcon;
    private int mAdChoiceResId;
    private boolean mIsCN;
    private int mAdChoiceCNResId;
    private SkipView skipView;
    private int skipMaxTime;

    private boolean canSkip;

    public PlayerView(ViewGroup container, OnPlayerListener listener, boolean isCN,boolean isInterstitial) {
        super(container.getContext());
        this.mListener = listener;
        skipMaxTime = isInterstitial ? 5 : 30;
        mIsCN = isCN;
        setId(CommonUtil.getResId(getContext(), "cp_player_view_id", "id"));
        setSaveEnabled(true);
        attachTo(container);

        mMainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {

                mCurrentPosition = msg.what;
                LogUtil.ownShow("MediaPlayer mCurrentPosition()..."+mCurrentPosition);
                if (mCurrentPosition <= 0) {
                    return;
                }

                //Control to show close button
                if (mCloseBtn == null && mShowCloseTime >= 0 && mCurrentPosition >= mShowCloseTime) {
                    showCloseButton();
                }

                if (!mIsVideoStart && !mIsVideoPlayCompletion) {
                    mIsVideoStart = true;
                    if (mListener != null) {
                        mListener.onVideoPlayStart();
                    }
                }
                if(canSkip){
                   int time = mCurrentPosition / 1000;
                   if(time > skipMaxTime){
                       skipView.showView();
                   }
                }

                if (mListener != null) {
                    mListener.onVideoUpdateProgress(mCurrentPosition);
                }

                if (!mVideoPlay25 && mCurrentPosition >= mVideoProgress25) {
                    mVideoPlay25 = true;
                    if (mListener != null) {
                        mListener.onVideoPlayProgress(25);
                    }
                } else if (!mVideoPlay50 && mCurrentPosition >= mVideoProgress50) {
                    mVideoPlay50 = true;
                    if (mListener != null) {
                        mListener.onVideoPlayProgress(50);
                    }
                } else if (!mVideoPlay75 && mCurrentPosition >= mVideoProgress75) {
                    mVideoPlay75 = true;
                    if (mListener != null) {
                        mListener.onVideoPlayProgress(75);
                    }
                }

                showView();
                if (mCountDownView != null && mCountDownView.isShown()) {
                    mCountDownView.refresh(mCurrentPosition);
                }
            }
        };
    }

    private void attachTo(ViewGroup container) {
        LayoutParams rl = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        container.addView(this, 0, rl);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        LogUtil.ownShow("onSaveInstanceState...");
        Parcelable parcelable = super.onSaveInstanceState();
        SavedState ss = new SavedState(parcelable);
        //Current video state
        ss.savePosition = mCurrentPosition;
        ss.saveVideoPlay25 = mVideoPlay25;
        ss.saveVideoPlay50 = mVideoPlay50;
        ss.saveVideoPlay75 = mVideoPlay75;
        ss.saveIsVideoStart = mIsVideoStart;
        ss.saveIsVideoPlayCompletion = mIsVideoPlayCompletion;
        ss.saveIsMute = mIsMute;

        LogUtil.ownShow("onSaveInstanceState..." + ss.print());
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        LogUtil.ownShow("onRestoreInstanceState...");
        if (state instanceof SavedState) {
            SavedState ss = (SavedState) state;
            LogUtil.ownShow("onRestoreInstanceState..." + ss.print());
            super.onRestoreInstanceState(ss.getSuperState());
            mCurrentPosition = ss.savePosition;
            mVideoPlay25 = ss.saveVideoPlay25;
            mVideoPlay50 = ss.saveVideoPlay50;
            mVideoPlay75 = ss.saveVideoPlay75;
            mIsVideoStart = ss.saveIsVideoStart;
            mIsVideoPlayCompletion = ss.saveIsVideoPlayCompletion;
            mIsMute = ss.saveIsMute;

            if (mMediaPlayer != null) {
                mMediaPlayer.setVolume(mIsMute ? 0 : 1, mIsMute ? 0 : 1);
            }
        }
    }

    static class SavedState extends BaseSavedState {

        int savePosition;
        boolean saveVideoPlay25;
        boolean saveVideoPlay50;
        boolean saveVideoPlay75;
        boolean saveIsVideoStart;
        boolean saveIsVideoPlayCompletion;
        boolean saveIsMute;

        public SavedState(Parcel source) {
            super(source);
            savePosition = source.readInt();
            boolean[] booleans = new boolean[6];
            source.readBooleanArray(booleans);
            saveVideoPlay25 = booleans[0];
            saveVideoPlay50 = booleans[1];
            saveVideoPlay75 = booleans[2];
            saveIsVideoStart = booleans[3];
            saveIsVideoPlayCompletion = booleans[4];
            saveIsMute = booleans[5];
        }


        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(savePosition);
            boolean[] booleans = new boolean[6];
            booleans[0] = saveVideoPlay25;
            booleans[1] = saveVideoPlay50;
            booleans[2] = saveVideoPlay75;
            booleans[3] = saveIsVideoStart;
            booleans[4] = saveIsVideoPlayCompletion;
            booleans[5] = saveIsMute;
            out.writeBooleanArray(booleans);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public String print() {
            return "SavedState(\n" +
                    "savePosition - " + savePosition + "\n" +
                    "saveVideoPlay25 - " + saveVideoPlay25 + "\n" +
                    "saveVideoPlay50 - " + saveVideoPlay50 + "\n" +
                    "saveVideoPlay75 - " + saveVideoPlay75 + "\n" +
                    "saveIsVideoStart - " + saveIsVideoStart + "\n" +
                    "saveIsVideoPlayCompletion - " + saveIsVideoPlayCompletion + "\n" +
                    "saveIsMute - " + saveIsMute + "\n)";
        }
    }


    public void setSetting(CPAdResponse cpAdResponse) {
        if (cpAdResponse == null) {
            return;
        }

        mIsMute = cpAdResponse.getVideo_mute() == 0;
        if(!mIsMute) {
            mIsMute = Audio.isAudioSilent(getContext());
        }
        mShowCloseTime = cpAdResponse.getShow_close_time() * 1000;
        LogUtil.ownShow("isMute - " + mIsMute);
        LogUtil.ownShow("showCloseTime - " + mShowCloseTime);
    }

    private void init() {
        LogUtil.ownShow("init...");
        boolean error = checkValid();
        if (error) {
            if (mListener != null) {
                mListener.onVideoShowFailed(CPErrorCode.get(CPErrorCode.rewardedVideoPlayVideoMissing, CPErrorCode.fail_video_file_error_));
            }
            return;
        }

        initParams();
        computeVideoSize();

        initTextureView();
        initMediaPlayer();
        initCountDownView();

        initMutebutton();
        initAdChoiceIcon();
        initSkipView();
    }

    private void initParams() {
        mViewSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mViewSizeDp, getContext().getResources().getDisplayMetrics());
        mViewMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mViewMarginDp, getContext().getResources().getDisplayMetrics());
        mLeftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mLeftMarginDp, getContext().getResources().getDisplayMetrics());
        mTopMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mTopMarginDp, getContext().getResources().getDisplayMetrics());

        mMuteResId = CommonUtil.getResId(getContext(), "cp_video_mute", "drawable");
        mAdChoiceResId = CommonUtil.getResId(getContext(), "ad", "drawable");
        mAdChoiceCNResId = CommonUtil.getResId(getContext(), "ad_cn", "drawable");
        mNoMuteResId = CommonUtil.getResId(getContext(), "cp_video_no_mute", "drawable");
        mCloseResId = CommonUtil.getResId(getContext(), "cp_video_close", "drawable");
    }

    private void computeVideoSize() {
        if (mVideoWidth != 0 && mVideoHeight != 0) {
            return;
        }
        try {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            CPVideoUtil.Size videoSize = CPVideoUtil.getAdaptiveVideoSize(mSourceFD, dm.widthPixels, dm.heightPixels);

            if (videoSize != null) {
                mVideoWidth = videoSize.width;
                mVideoHeight = videoSize.height;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initCountDownView() {
        if (getChildAt(mCountDownViewIndex) != null) {
            removeViewAt(mCountDownViewIndex);
        }

        mCountDownView = new CountDownView(getContext());
        mCountDownView.setId(CommonUtil.getResId(getContext(), "cp_count_down_view_id", "id"));
        LayoutParams rl = new LayoutParams(mViewSize, mViewSize);
        rl.leftMargin = mLeftMargin;
        rl.topMargin = mTopMargin;
        mCountDownView.setVisibility(View.INVISIBLE);
        addView(mCountDownView, mCountDownViewIndex, rl);
    }

    private void initSkipView() {
        if (getChildAt(mSkipIndex) != null) {
            removeViewAt(mSkipIndex);
        }

        skipView = new SkipView(getContext());
        skipView.setId(CommonUtil.getResId(getContext(), "cp_skip_view_id", "id"));
        LayoutParams rl = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rl.rightMargin = mLeftMargin;
        rl.topMargin = mTopMargin;
        skipView.init(getContext(),mListener);
        addView(skipView, rl);
    }

    private void initAdChoiceIcon() {
        if (getChildAt(mAdChoiceIconIndex) != null) {
            removeViewAt(mAdChoiceIconIndex);
        }

        mAdChoiceIcon = new ImageView(getContext());
        mAdChoiceIcon.setId(CommonUtil.getResId(getContext(), "cp_ad_choice_id", "id"));
        LayoutParams rl = new LayoutParams(30, 30);
        rl.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mAdChoiceIcon.setVisibility(VISIBLE);
        addView(mAdChoiceIcon, rl);
        if (mIsCN) {
            mAdChoiceIcon.setBackgroundResource(mAdChoiceCNResId);
        } else {
            mAdChoiceIcon.setBackgroundResource(mAdChoiceResId);
        }
    }

    private void initMutebutton() {
        if (getChildAt(mMuteButtonIndex) != null) {
            removeViewAt(mMuteButtonIndex);
        }

        mMuteBtn = new ImageView(getContext());
        mMuteBtn.setId(CommonUtil.getResId(getContext(), "cp_btn_mute_id", "id"));
        LayoutParams rl = new LayoutParams(mViewSize, mViewSize);
        rl.topMargin = mTopMargin;
        rl.leftMargin = mViewMargin;
        mMuteBtn.setVisibility(View.INVISIBLE);
        addView(mMuteBtn, mMuteButtonIndex, rl);


        if (mIsMute) {
            mMuteBtn.setBackgroundResource(mMuteResId);
        } else {
            mMuteBtn.setBackgroundResource(mNoMuteResId);
        }

        mMuteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsVideoPlayCompletion) {
                    return;
                }

                mIsMute = !mIsMute;
                if (mIsMute) {//静音
                    mMuteBtn.setBackgroundResource(mMuteResId);
                    if (mMediaPlayer != null) {
                        mMediaPlayer.setVolume(0f, 0f);
                    }
                } else {
                    mMuteBtn.setBackgroundResource(mNoMuteResId);
                    if (mMediaPlayer != null) {
                        mMediaPlayer.setVolume(1f, 1f);
                    }
                }

            }
        });
    }

    private void initCloseButton() {
        if (getChildAt(mCloseButtonIndex) != null) {
            removeViewAt(mCloseButtonIndex);
        }

        mCloseBtn = new ImageView(getContext());
        mCloseBtn.setId(CommonUtil.getResId(getContext(), "cp_btn_close_id", "id"));
        LayoutParams rl = new LayoutParams(mViewSize, mViewSize);
        rl.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rl.rightMargin = mViewMargin;
        rl.addRule(RelativeLayout.ALIGN_TOP, mCountDownView.getId());
        rl.addRule(RelativeLayout.ALIGN_BOTTOM, mCountDownView.getId());
        addView(mCloseBtn, mCloseButtonIndex, rl);

        mCloseBtn.setImageResource(mCloseResId);

        ViewUtil.expandTouchArea(mCloseBtn, mViewSize / 2);

        mCloseBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onVideoCloseClick();
                }
            }
        });
    }

    private void showView() {
        showCountDownView();
        showMuteButton();
    }

    private void showCountDownView() {
        if (mCountDownView != null && !mCountDownView.isShown()) {
            mCountDownView.setVisibility(View.VISIBLE);
        }
    }

    private void showMuteButton() {
        if (mMuteBtn != null && !mMuteBtn.isShown()) {
            mMuteBtn.setVisibility(View.VISIBLE);
        }
    }


    private void startProgressThread() {
        if (mProgressThread != null) {
            return;
        }
        mFlag = true;
        mProgressThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mFlag) {
                    if (!mIsVideoPlayCompletion && mMediaPlayer != null && mMediaPlayer.isPlaying() && mMainHandler != null) {

                        mMainHandler.sendEmptyMessage(mMediaPlayer.getCurrentPosition());
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        mProgressThread.start();
    }

    private void stopProgressThread() {
        mFlag = false;
        mProgressThread = null;
    }

    public void load(String url) {
        this.mSourcePath = url;

        init();
    }

    private boolean checkValid() {
        mFileInputStream = CPResourceManager.getInstance().getInputStream(mSourcePath);
        boolean error = false;

        try {
            if (mFileInputStream == null) {
                error = true;
            } else {
                mSourceFD = mFileInputStream.getFD();
            }

        } catch (Throwable e) {
            e.printStackTrace();
            error = true;
        }
        if (error) {
            if (mFileInputStream != null) {
                try {
                    mFileInputStream.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        return error;
    }



    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setVolume(mIsMute ? 0 : 1, mIsMute ? 0 : 1);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    LogUtil.ownShow("MediaPlayer onPrepared()...");

                    mIsMediaPlayerPrepared = true;
                    mDuration = mMediaPlayer.getDuration();
                    LogUtil.ownShow("MediaPlayer mDuration()..."+mDuration);
                    canSkip = mDuration / 1000 > skipMaxTime;
                    if (mCountDownView != null) {
                        mCountDownView.setDuration(mDuration);
                    }
                    mVideoProgress25 = Math.round(0.25f * mDuration);
                    mVideoProgress50 = Math.round(0.5f * mDuration);
                    mVideoProgress75 = Math.round(0.75f * mDuration);


                    if (mCurrentPosition > 0) {
                        mMediaPlayer.seekTo(mCurrentPosition);
                    } else {

                        start();
                    }
                }
            });

            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    start();
                }
            });

            if (!mIsVideoPlayCompletion) {
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopProgressThread();
                        mIsVideoPlayCompletion = true;
                        mCurrentPosition = mDuration;

                        if (mListener != null) {
                            mListener.onVideoPlayCompletion();
                        }
                    }
                });
            }

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (mListener != null) {
                        mListener.onVideoShowFailed(CPErrorCode.get(CPErrorCode.rewardedVideoPlayError, CPErrorCode.fail_player));
                    }
                    return true;//false will call OnCompletionListener
                }
            });

        }
    }


    private void initTextureView() {
        if (mTextureView == null) {
            mTextureView = new TextureView(getContext());
            mTextureView.setSurfaceTextureListener(this);
            mTextureView.setKeepScreenOn(true);

            LayoutParams rl = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                rl.width = mVideoWidth;
                rl.height = mVideoHeight;
            }
            rl.addRule(RelativeLayout.CENTER_IN_PARENT);
            removeAllViews();
            addView(mTextureView, rl);

            mTextureView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onVideoClick();
                    }
                }
            });
        }
    }


    private void openPlayer() {
        init();
        try {
            mMediaPlayer.reset();

            if (!mSourceFD.valid()) {
                throw new IllegalStateException("cp video resource is valid");
            } else {
                LogUtil.ownShow("video resource valid - " + mSourceFD.valid());
            }

            mMediaPlayer.setDataSource(this.mSourceFD);
            try {
                if (mFileInputStream != null) {
                    mFileInputStream.close();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            if (mSurface == null) {
                mSurface = new Surface(mSurfaceTexture);
            }
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.prepareAsync();

        } catch (Throwable e) {
            e.printStackTrace();
            if (mListener != null) {
                mListener.onVideoShowFailed(CPErrorCode.get(CPErrorCode.rewardedVideoPlayError, e.getMessage()));
            }
        }
    }

    public void showCloseButton() {
        initCloseButton();
    }


    public void start() {
        LogUtil.ownShow("start()");
        if (mMediaPlayer != null && mIsMediaPlayerPrepared) {
            mMediaPlayer.start();
        }
        startProgressThread();
    }

    public void pause() {
        LogUtil.ownShow("pause()");
        stopProgressThread();
        if (isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    public void stop() {
        LogUtil.ownShow("stop()");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }

//        if (mListener != null) {
//            mListener.onVideoPlayEnd();
//        }
    }

    public void release() {
        if (!mIsMediaPlayerPrepared) {
            return;
        }
        LogUtil.ownShow("release...");
        stopProgressThread();
        mSurfaceTexture = null;
        mSurface = null;
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
        }
        mIsMediaPlayerPrepared = false;
    }

    public boolean isPlaying() {
        if (mMediaPlayer != null && mIsMediaPlayerPrepared) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        LogUtil.ownShow("onSurfaceTextureAvailable()...");
        mSurfaceTexture = surface;
        openPlayer();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        LogUtil.ownShow("onSurfaceTextureDestroyed()...");
        this.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LogUtil.ownShow("onDetachedFromWindow()...");
        this.release();
    }

    public interface OnPlayerListener {
        void onVideoPlayStart();

        void onVideoUpdateProgress(int progress);

        void onVideoPlayEnd();

        void onVideoPlayCompletion();

        void onVideoShowFailed(CPError error);

        void onVideoPlayProgress(int progressArea);

        void onVideoCloseClick();

        void onVideoClick();

        void onVideoSkip();
    }
}
