package com.aritxonly.cloudlane

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aritxonly.cloudlane.data.Photo
import com.aritxonly.cloudlane.model.AdvancedMaterialFineTuning
import com.aritxonly.cloudlane.ui.theme.*
import com.aritxonly.cloudlane.ui.material.glass.*
import com.aritxonly.cloudlane.ui.navigation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.*

class MainActivity: ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);enableEdgeToEdge();setContent { CloudlaneApp() } }
}

@Composable
fun CloudlaneApp(model:LibraryModel=viewModel()) {
 val state by model.state.collectAsStateWithLifecycle()
 val context=LocalContext.current
 var page by rememberSaveable { mutableStateOf("home") }
 var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
 var pending by rememberSaveable { mutableStateOf(emptyList<String>()) }
 var feedback by rememberSaveable { mutableStateOf("") }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { model.scan() }
 val trash=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
  if(result.resultCode==Activity.RESULT_OK) { model.removed(pending);selected=emptyList();feedback="已移入本机回收站" }
  else feedback="已取消清理"
  pending=emptyList()
 }
 CloudlaneTheme {
  val c=MiuixTheme.colorScheme
   val backdrop=rememberLayerBackdrop()
   val list=rememberLazyListState()
   val progress by remember { derivedStateOf { if(list.firstVisibleItemIndex>0) 1f else (list.firstVisibleItemScrollOffset/160f).coerceIn(0f,1f) } }
   LaunchedEffect(page) { list.scrollToItem(0);selected=emptyList() }
   val spec=remember { createAdvancedMaterialSpec(true,24f,AdvancedMaterialFineTuning.Default) }
   CompositionLocalProvider(LocalAdvancedMaterialSpec provides spec,LocalAdvancedMaterialBackdrop provides backdrop,LocalTopBarScrollProgress provides progress,LocalTopBarButtonMaterialProgress provides progress) {
    Scaffold(containerColor=c.surface,
     topBar={ ProgressiveTopBarMaterial { SmallTopAppBar(title=when(page){"clean"->"清理";"settings"->"设置";else->"Cloudlane"},color=Color.Transparent,actions={
      SoftGlassIconButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:${context.packageName}")))},materialAlpha=progress) { Icon(painterResource(R.drawable.hmi_settings),"照片权限与应用设置") }
      Spacer(Modifier.width(16.dp))
     }) } },
     bottomBar={ Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal=20.dp,vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
      val tabs=listOf(Triple("home","概览",R.drawable.hmi_cloud),Triple("clean","清理",R.drawable.hmi_delete),Triple("settings","设置",R.drawable.hmi_settings)).map { (key,label,icon)->MiuixFloatingTabItem(key,label,painterResource(icon),painterResource(icon)) }
      MiuixFloatingTabBar(tabs,page,{page=it.key},Modifier.weight(1f),layout=MiuixFloatingTabLayout.Stacked)
      SoftGlassIconButton(onClick={permission.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))},enabled=!state.busy,size=54.dp) { Icon(painterResource(R.drawable.hmi_autorenew),if(state.busy) "分析中" else "扫描本机照片") }
     } }
    ) { padding ->
     LazyColumn(state=list,modifier=Modifier.fillMaxSize().layerBackdrop(backdrop),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=padding.calculateTopPadding()+12.dp,bottom=padding.calculateBottomPadding()+20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
      item { Text(if(page=="clean") "自动发现，清理由你决定" else if(page=="settings") "按自己的节奏整理" else "给相册留一点空间",style=MiuixTheme.textStyles.title2) }
      item { Text(state.message,style=MiuixTheme.textStyles.body2,color=c.onSurfaceVariantSummary) }
      if(feedback.isNotBlank()) item { Text(feedback) }
      when(page) {
       "home" -> {
        item { InfoCard("本机照片","${state.total} 张可访问照片 · ${state.photos.size} 项待复查") }
        item { InfoCard("夜间整理",if(state.night) "00:00–07:00 · 充电且连接非计费网络时分析" else "尚未开启，可在设置中启用") }
        item { InfoCard("iCloud 照片","尚未接入。当前版本只分析本机照片，不上传或删除云端内容。") }
        item { Button(onClick={permission.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))},enabled=!state.busy,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColorsPrimary()) { Text(if(state.busy) "正在分析…" else "授权并扫描照片") } }
        item { Button(onClick={page="clean"},modifier=Modifier.fillMaxWidth()) { Text("查看清理建议") } }
       }
       "clean" -> {
        if(state.photos.isEmpty()) item { InfoCard("暂时没有清理建议","扫描后将在这里显示旧截图、疑似临时码和完全重复的照片。年龄只作为复查线索。") }
        if(selected.isNotEmpty()) item { Button(onClick={
         try {
          pending=selected.take(100)
          val intent=MediaStore.createTrashRequest(context.contentResolver,pending.map(Uri::parse),true)
          trash.launch(IntentSenderRequest.Builder(intent.intentSender).build())
         } catch(e:Exception) { pending=emptyList();feedback="无法请求清理，请重新扫描并检查照片权限" }
        },modifier=Modifier.fillMaxWidth()) { Text("将 ${selected.size} 张移入本机回收站") } }
        items(state.photos,key={it.uri}) { photo -> PhotoCard(photo,photo.uri in selected,onSelect={selected=if(photo.uri in selected) selected-photo.uri else if(selected.size<100) selected+photo.uri else selected},onKeep={model.keep(photo.uri);selected=selected-photo.uri},onPreview={
         try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(photo.uri),"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch(e:Exception) { feedback="没有可用的图片查看器，或照片访问权限已变化" }
        }) }
       }
       else -> {
        item { Card { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("夜间分析",style=MiuixTheme.textStyles.title4);Text("00:00–07:00，充电且连接非计费网络时分批分析。系统可能延迟执行。不会自动删除照片。");Switch(checked=state.night,onCheckedChange=model::night) } } }
        item { InfoCard("本地隐私","图片与识别文字在设备上处理。OCR 文本仅用于本次分类，不写入分析索引。") }
        item { Button(onClick=model::resetKept,modifier=Modifier.fillMaxWidth()) { Text("恢复保留名单中的推荐") } }
        item { InfoCard("同步状态","iCloud 双向同步正在开发；本地扫描不代表照片已备份。") }
        item { InfoCard("关于","Cloudlane 0.1.0 · MIUIX Soft Glass\nAndroid 13 及以上") }
       }
      }
     }
    }
   }
 }
}

@Composable private fun InfoCard(title:String,summary:String) {
 Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { Text(title,style=MiuixTheme.textStyles.title4);Text(summary,style=MiuixTheme.textStyles.body1,color=MiuixTheme.colorScheme.onSurfaceVariantSummary) } }
}
@Composable private fun PhotoCard(photo:Photo,selected:Boolean,onSelect:()->Unit,onKeep:()->Unit,onPreview:()->Unit) {
 val context=LocalContext.current
 val thumbnail by produceState<android.graphics.Bitmap?>(null,photo.uri,photo.modified) { value=withContext(Dispatchers.IO) { runCatching { context.contentResolver.loadThumbnail(Uri.parse(photo.uri),android.util.Size(180,180),null) }.getOrNull() } }
 Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
   thumbnail?.let { Image(it.asImageBitmap(),"照片缩略图",Modifier.size(64.dp)) }
   Column(Modifier.weight(1f)) { Text(photo.name,maxLines=2);Text("${photo.size/1024} KB",style=MiuixTheme.textStyles.footnote1) }
  }
  Text(photo.reason,style=MiuixTheme.textStyles.body2)
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   Button(onClick=onSelect,modifier=Modifier.weight(1f)) { Text(if(selected) "取消" else "选择") }
   Button(onClick=onKeep,modifier=Modifier.weight(1f)) { Text("保留") }
   Button(onClick=onPreview,modifier=Modifier.weight(1f)) { Text("查看") }
  }
 } }
}
