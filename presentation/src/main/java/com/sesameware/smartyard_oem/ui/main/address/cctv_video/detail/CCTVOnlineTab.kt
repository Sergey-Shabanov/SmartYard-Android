package com.sesameware.smartyard_oem.ui.main.address.cctv_video.detail

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.MimeTypes
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvDetailOnlineBinding
import com.sesameware.smartyard_oem.ui.main.ExitFullscreenListener
import com.sesameware.smartyard_oem.ui.main.MainActivity
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.CCTVViewModel
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.ZoomLayout
import com.sesameware.smartyard_oem.ui.main.address.cctv_video.adapters.DetailButtonsAdapter
import timber.log.Timber

class CCTVOnlineTab : Fragment(), ExitFullscreenListener {
    private var _binding: FragmentCctvDetailOnlineBinding? = null
    private val binding get() = _binding!!

    private var mPlayer: SimpleExoPlayer? = null
    private var forceVideoTrack = true  //принудительное использование треков с высоким разрешением
    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()
    private var mExoPlayerFullscreen = false

    //для полноэкранного режима
    private var lpVideoWrap: ViewGroup.LayoutParams? = null
    private var playerResizeMode: Int = 0

    private var canRenewToken = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MainActivity)?.setExitFullscreenListener(this)
        _binding = FragmentCctvDetailOnlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        (activity as? MainActivity)?.setExitFullscreenListener(null)

        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Timber.d("debug_dmm __onViewCreated")
        setupAdapter(mCCTVViewModel.cameraList.value, mCCTVViewModel.chosenIndex.value)
    }

    private fun setFullscreenMode() {
        if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            lpVideoWrap = LinearLayout.LayoutParams(binding.videoWrap.layoutParams as LinearLayout.LayoutParams)
            (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)

            (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.INVISIBLE
            (activity as? MainActivity)?.binding?.llMain?.addView(binding.videoWrap, 0)

            (activity as? MainActivity)?.hideSystemUI()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            playerResizeMode = binding.mVideoView.resizeMode
            binding.mVideoView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            binding.mFullScreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_exit_fullscreen)
            binding.videoWrap.background = null
            (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(Color.BLACK)

            val lp = binding.videoWrap.layoutParams as LinearLayout.LayoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT
            lp.topMargin = 0
            binding.videoWrap.layoutParams = lp
            binding.videoWrap.requestLayout()
            (binding.mVideoView.parent as ZoomLayout).resetZoom()
        }
    }

    private fun setNormalMode() {
        if (activity?.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            (binding.videoWrap.parent as ViewGroup).removeView(binding.videoWrap)
            (activity as? MainActivity)?.binding?.relativeLayout?.visibility = View.VISIBLE
            binding.llVideoPlayback.addView(binding.videoWrap, 0)

            (activity as? MainActivity)?.showSystemUI()

            binding.mVideoView.resizeMode = playerResizeMode
            binding.mFullScreen.background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cctv_enter_fullscreen)

            binding.videoWrap.background = ContextCompat.getDrawable(requireContext(), R.drawable.background_radius_video_clip)

            //возвращаем дефолтные layouts
            if (lpVideoWrap != null) {
                binding.videoWrap.layoutParams = lpVideoWrap
                binding.videoWrap.requestLayout()
            }
            (binding.mVideoView.parent as ZoomLayout).resetZoom()

            (activity as? MainActivity)?.binding?.llMain?.background = ColorDrawable(ContextCompat.getColor(requireContext(), R.color.white_200))
        }
    }

    private fun setupAdapter(currentList: List<CCTVData>?, currentIndex: Int?) {
        val lm = GridLayoutManager(context, 5)
        binding.recyclerView.layoutManager = lm
        if (currentIndex != null && currentList != null) {
            val spacingHor = resources.getDimensionPixelSize(R.dimen.cctv_buttons_hor)
            val spacingVer = resources.getDimensionPixelSize(R.dimen.cctv_buttons_ver)
            binding.recyclerView.addItemDecoration(GridSpacingItemDecoration(5, spacingHor, spacingVer))
            binding.recyclerView.adapter = DetailButtonsAdapter(
                requireContext(),
                currentIndex,
                currentList
            ) {
                mCCTVViewModel.chooseCamera(it)
            }
        }
    }

    private fun setupObserve() {
        mCCTVViewModel.chosenCamera.observe(
            viewLifecycleOwner
        ) {
            it?.run {
                changeVideoSource(hls)
            }
        }

        mCCTVViewModel.stateFullScreen.observe(
            viewLifecycleOwner
        ) {
            mExoPlayerFullscreen = it
            if (mCCTVViewModel.currentTabId == CCTVViewModel.ONLINE_TAB_POSITION) {
                if (it) {
                    setFullscreenMode()
                } else {
                    setNormalMode()
                }
            }
        }
    }

    private fun createPlayer(
        videoView: PlayerView,
        progressView: ProgressBar
    ): SimpleExoPlayer {
        Timber.d("debug_dmm createPlayer()")

        val trackSelector = DefaultTrackSelector(requireContext())
        val player  = SimpleExoPlayer.Builder(requireContext())
            .setTrackSelector(trackSelector)
            .build()

        //для теста
        //player.addAnalyticsListener(EventLogger(trackSelector))

        videoView.player = player
        videoView.useController = false
        player.playWhenReady = true

        val p = videoView.parent as ViewGroup
        p.removeView(videoView)
        p.addView(videoView, 0)

        binding.mFullScreen.setOnClickListener {
            mCCTVViewModel.fullScreen(!mExoPlayerFullscreen)
        }

        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(
                playWhenReady: Boolean,
                playbackState: Int
            ) {
                if (playbackState == Player.STATE_READY) {
                    canRenewToken = true
                    mPlayer?.videoFormat?.let {
                        if (it.width > 0 && it.height > 0) {
                            (binding.mVideoView.parent as ZoomLayout).setAspectRatio(it.width.toFloat() / it.height.toFloat())
                        }
                    }
                }

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                progressView.visibility = when (playbackState) {
                    Player.STATE_BUFFERING -> View.VISIBLE
                    else -> View.GONE
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                    if (canRenewToken) {
                        canRenewToken = false

                        //перезапрашиваем список камер
                        mCCTVViewModel.cctvModel.value?.let {
                            mCCTVViewModel.refreshCameras(it)
                        }
                    } else {
                        mCCTVViewModel.showGlobalError(error.sourceException)
                    }
                }

                if (error.type == ExoPlaybackException.TYPE_RENDERER) {
                    if (forceVideoTrack) {
                        forceVideoTrack = false
                        releasePlayer()
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        initPlayer()
                    }
                }
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray,
                                         trackSelections: TrackSelectionArray) {
                super.onTracksChanged(trackGroups, trackSelections)

                if (!forceVideoTrack) {
                    return
                }

                val decoderInfo = MediaCodecUtil.getDecoderInfo(MimeTypes.VIDEO_H264, false, false)
                val maxSupportedWidth = (decoderInfo?.capabilities?.videoCapabilities?.supportedWidths?.upper ?: 0) * RESOLUTION_TOLERANCE
                val maxSupportedHeight = (decoderInfo?.capabilities?.videoCapabilities?.supportedHeights?.upper ?: 0) * RESOLUTION_TOLERANCE

                (player.trackSelector as? DefaultTrackSelector)?.let{ trackSelector ->
                    trackSelector.currentMappedTrackInfo?.let { mappedTrackInfo ->
                        for (k in 0 until mappedTrackInfo.rendererCount) {
                            if (mappedTrackInfo.getRendererType(k) == C.TRACK_TYPE_VIDEO) {
                                val rendererTrackGroups = mappedTrackInfo.getTrackGroups(k)
                                for (i in 0 until rendererTrackGroups.length) {
                                    val tracks = mutableListOf<Int>()
                                    for (j in 0 until rendererTrackGroups[i].length) {
                                        if (mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_HANDLED ||
                                            mappedTrackInfo.getTrackSupport(k, i, j) == C.FORMAT_EXCEEDS_CAPABILITIES &&
                                            (maxSupportedWidth >= rendererTrackGroups[i].getFormat(j).width ||
                                                    maxSupportedHeight >= rendererTrackGroups[i].getFormat(j).height)) {
                                            tracks.add(j)
                                        }
                                    }
                                    val selectionOverride = DefaultTrackSelector.SelectionOverride(i, *tracks.toIntArray())
                                    trackSelector.setParameters(
                                        trackSelector.buildUponParameters()
                                            .setSelectionOverride(k, rendererTrackGroups, selectionOverride)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        })
        return player
    }

    private fun changeVideoSource(hls_url: String) {
        mPlayer?.let { player ->
            binding.mProgress.visibility = View.VISIBLE
            player.setMediaItem(MediaItem.fromUri(Uri.parse(hls_url)))
            player.prepare()
        }
    }

    fun releasePlayer() {
        Timber.d("debug_dmm releasePlayer()")
        Timber.d("debug_dmm mPlayer = $mPlayer")
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer = null
    }

    fun initPlayer() {
        if (mPlayer == null && view != null) {
            mPlayer = createPlayer(binding.mVideoView, binding.mProgress)
            setupObserve()
            binding.videoWrap.clipToOutline = true
        }
    }

    companion object {
        fun newInstance() = CCTVOnlineTab().apply {
            Timber.d("debug_dmm __new instance $this")
        }

        const val RESOLUTION_TOLERANCE = 1.08  // коэффициент допуска видео разрешения
    }

    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val horSpacing: Int,
        private val verSpacing: Int
    ) :
        ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount
            outRect.left = column * horSpacing / spanCount
            outRect.right =
                horSpacing - (column + 1) * horSpacing / spanCount
            if (position >= spanCount) {
                outRect.top = verSpacing
            }
        }
    }

    override fun onExitFullscreen() {
        if (mExoPlayerFullscreen) {
            mCCTVViewModel.fullScreen(false)
            mExoPlayerFullscreen = false
            setNormalMode()
        }
    }

    override fun onPause() {
        super.onPause()

        Timber.d("debug_dmm __onPause")
        releasePlayer()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()

        Timber.d("debug_dmm __onResume, is fragment hidden = $isHidden")

        if ((activity as? MainActivity)?.binding?.bottomNav?.selectedItemId == R.id.address && mCCTVViewModel.currentTabId == CCTVViewModel.ONLINE_TAB_POSITION) {
            initPlayer()
            Timber.d("debug_dmm __CCTVOnlineTab: $this")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        (binding.mVideoView.parent as ZoomLayout).resetZoom()
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        Timber.d("debug_dmm  __isVisibleToUser = $isVisibleToUser")
    }
}
