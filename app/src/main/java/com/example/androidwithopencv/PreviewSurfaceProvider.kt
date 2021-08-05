package com.example.androidwithopencv

import android.util.Log
import android.view.Surface
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.core.util.Consumer
import java.util.concurrent.Executor

class PreviewSurfaceProvider(val surface: Surface, val executor: Executor) : Preview.SurfaceProvider {

    override fun onSurfaceRequested(request: SurfaceRequest) {
        request.provideSurface(surface, executor, Consumer { result: SurfaceRequest.Result ->  {
            Log.d("FAFA", "PreviewSurfaceProvider onSurfaceRequested consumer result : ${result.resultCode}")
        }})
    }


}
