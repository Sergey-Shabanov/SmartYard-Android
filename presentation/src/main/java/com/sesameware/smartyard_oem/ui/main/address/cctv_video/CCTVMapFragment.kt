package com.sesameware.smartyard_oem.ui.main.address.cctv_video

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import org.koin.androidx.viewmodel.ext.android.sharedStateViewModel
import com.sesameware.domain.model.response.CCTVData
import com.sesameware.smartyard_oem.MapFragment
import com.sesameware.smartyard_oem.R
import com.sesameware.smartyard_oem.databinding.FragmentCctvMapBinding
import com.sesameware.smartyard_oem.toLatLng
import com.sesameware.smartyard_oem.ui.map.MapProvider
import com.sesameware.smartyard_oem.ui.map.MapSettings
import com.sesameware.smartyard_oem.ui.map.MarkerData
import com.sesameware.smartyard_oem.ui.map.MarkerType

class CCTVMapFragment : MapFragment() {
    private var _binding: FragmentCctvMapBinding? = null
    private val binding get() = _binding!!

    private val mCCTVViewModel: CCTVViewModel by sharedStateViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCctvMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        context?.let { context ->
            setupUi(context)
        }
    }

    private fun setupUi(context: Context) {
        binding.contentWrap.clipToOutline = true
        binding.ivBack.setOnClickListener {
            this.findNavController().popBackStack()
        }
        binding.tvTitleSub.text = mCCTVViewModel.cctvModel.value?.address
        mCCTVViewModel.cameraList.value?.let { list ->
            createMapProvider(context, list)
        }
    }

    private fun createMapProvider(
        context: Context,
        list: List<CCTVData>
    ) {
        val mapProvider = MapProvider(context)
        this.mapProvider = mapProvider
        val settings = MapSettings(context) { marker ->
            marker.index?.let {
                mCCTVViewModel.chooseCamera(it)
                this.findNavController().navigate(R.id.action_CCTVMapFragment_to_CCTVDetailFragment)
            }
            true
        }
        val listMarker = list.mapIndexed { index, item ->
            MarkerData(MarkerType.Camera, item.toLatLng(), "", index)
        }
        binding.contentWrap.removeAllViews()
        binding.contentWrap.addView(mapProvider)
        mapProvider.createMap(this, settings) {
            it.placeMarkers(
                listMarker,
                instant = true
            )
        }
    }
}
