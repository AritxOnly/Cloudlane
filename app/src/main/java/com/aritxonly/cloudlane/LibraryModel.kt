package com.aritxonly.cloudlane

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aritxonly.cloudlane.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LibraryState(val photos: List<Photo> = emptyList(),val total: Int=0,val busy: Boolean=false,val message: String="授权后开始分析本机照片",val night: Boolean=false)
class LibraryModel(app: Application): AndroidViewModel(app) {
    private val mutable=MutableStateFlow(LibraryState());val state=mutable.asStateFlow()
    init { reload() }
    fun reload(message: String?=null) { viewModelScope.launch(Dispatchers.IO) {
        val context=getApplication<Application>();val prefs=context.getSharedPreferences("settings",0)
        val photos=PhotoIndex(context).use { it.candidates() }
        mutable.update { it.copy(photos=photos,total=prefs.getInt("total",0),night=prefs.getBoolean("night",false),message=message ?: if (prefs.getLong("lastScan", 0) > 0) "已有本机分析结果，可继续扫描新照片" else it.message) }
    } }
    fun scan() {
        if(mutable.value.busy)return
        mutable.update { it.copy(busy=true,message="正在读取照片并分析，每批最多 80 张新照片…") }
        viewModelScope.launch {
            try { PhotoScanner(getApplication()).scan();reload("本批分析完成；再次扫描会继续处理尚未分析的照片") }
            catch(e: CancellationException) { throw e }
            catch(e: SecurityException) { mutable.update { it.copy(message="需要照片访问权限；部分授权仅分析所选照片") } }
            catch(e: Exception) { mutable.update { it.copy(message="分析失败：${e.localizedMessage ?: "请重试"}") } }
            finally { mutable.update { it.copy(busy=false) } }
        }
    }
    fun keep(uri:String) { viewModelScope.launch(Dispatchers.IO) { PhotoIndex(getApplication()).use { it.keep(uri) };reload("已加入保留名单") } }
    fun removed(uris:List<String>) { viewModelScope.launch(Dispatchers.IO) { PhotoIndex(getApplication()).use { it.remove(uris) };val prefs=getApplication<Application>().getSharedPreferences("settings",0);prefs.edit().putInt("total",(prefs.getInt("total",0)-uris.size).coerceAtLeast(0)).apply();reload("已完成系统回收站操作；云端照片未修改") } }
    fun resetKept() { viewModelScope.launch(Dispatchers.IO) { PhotoIndex(getApplication()).use { it.resetKept() };reload("已恢复保留图片的推荐") } }
    fun night(enabled:Boolean) { NightWorker.configure(getApplication(),enabled);mutable.update { it.copy(night=enabled) } }
}
