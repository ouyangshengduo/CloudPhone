package com.cloud.phone.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.cloud.phone.model.BaseMessage;
import com.cloud.phone.model.Message;
import com.cloud.phone.model.Room;
import com.cloud.phone.model.RoomType;
import com.cloud.phone.util.LogUtil;
import com.cloud.phone.util.PreferenceUtil;
import com.cloud.phone.webrtc.WebRtcInterface;
import com.cloud.phone.webrtc.WebRtcManager;

import org.webrtc.MediaStream;

import java.util.ArrayList;
import java.util.List;

/**
 * @author senduo.ouyang
 * @date 2021-10-11
 * @version 1.0
 * @description 主activity
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener, ViewCallback {

    private Button shareScreen;
    private Button shareCamera1;
    private Button shareCamera2;
    private int defaultCheck = 1000;
    private int PROJECTION_REQUEST_CODE = 100;
    private EditText etWidth;
    private EditText etHeight;
    private EditText etFps;
    private EditText etWebsocketAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        permissionCheck(defaultCheck);
    }

    private void init(){
        shareScreen = findViewById(R.id.share_screen);
        shareCamera1  = findViewById(R.id.share_camera1);
        shareCamera2 = findViewById(R.id.share_camera2);
        etWebsocketAddress = findViewById(R.id.et_websocket_address);
        etWidth = findViewById(R.id.et_width);
        etHeight = findViewById(R.id.et_height);
        etFps = findViewById(R.id.et_fps);
        shareScreen.setOnClickListener(this);
        shareCamera1.setOnClickListener(this);
        shareCamera2.setOnClickListener(this);

        String webSocketAddress = PreferenceUtil.getInstance().getString("webSocketAddress","");
        String widthStr = PreferenceUtil.getInstance().getString("width","");
        String heightStr = PreferenceUtil.getInstance().getString("height","");
        String fpsStr = PreferenceUtil.getInstance().getString("fps","");

        etWebsocketAddress.setText(webSocketAddress);
        etWidth.setText(widthStr);
        etHeight.setText(heightStr);
        etFps.setText(fpsStr);


    }

    private void permissionCheck(int code){
        List<String> permissions = permissionCheck();
        if (!permissions.isEmpty()){
            permissionRequest(permissions,code);
        }else if (code != defaultCheck){
            sendRequest(code);
        }
    }

    //判断是否授权所有权限
    private List<String> permissionCheck(){
        List<String> permissions = new ArrayList<>();
        if (!checkPermission(Manifest.permission.CAMERA)){
            permissions.add(Manifest.permission.CAMERA);
        }
        if (!checkPermission(Manifest.permission.RECORD_AUDIO)){
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions;
    }

    //发起权限申请
    private void permissionRequest(List<String> permissions,int requestCode){
        String[] permissionArray = permissions.toArray(new String[permissions.size()]);
        ActivityCompat.requestPermissions(this,permissionArray,requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == defaultCheck){
            if (grantResults.length >0){
                for (int result:grantResults){
                    if (result != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this,"您拒绝了权限相关功能将无法使用！",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }else {
                Toast.makeText(MainActivity.this,"发生未知错误！",Toast.LENGTH_SHORT).show();
            }
        }else{
            if (grantResults.length >0){
                for (int result:grantResults){
                    if (result != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this,"对不起，您拒绝了权限无法使用此功能！",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                sendRequest(requestCode);
            }else {
                Toast.makeText(MainActivity.this,"发生未知错误！",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode != PROJECTION_REQUEST_CODE){
            Toast.makeText(this,"unknow request code: " + requestCode,Toast.LENGTH_SHORT);
            return;
        }
        if(resultCode != RESULT_OK){
            Toast.makeText(this,"permission denied !",Toast.LENGTH_SHORT);
            return;
        }

        WebRtcManager.getInstance(this,null).projectionResultData = data;
        sendRequest(RoomType.SCREEN.getCode());
    }

    //判断是否有权限
    private boolean checkPermission(String permission){
        return ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void permissionCheckForProjection(){
        saveData();
        MediaProjectionManager projectionManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent,PROJECTION_REQUEST_CODE);
    }

    private void saveData() {
        PreferenceUtil.getInstance().setString("webSocketAddress",etWebsocketAddress.getText().toString().trim());
        PreferenceUtil.getInstance().setString("width",etWidth.getText().toString().trim());
        PreferenceUtil.getInstance().setString("height",etHeight.getText().toString().trim());
        PreferenceUtil.getInstance().setString("fps",etFps.getText().toString().trim());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.share_screen:
                permissionCheckForProjection();
                break;
            case R.id.share_camera1:
                sendRequest(RoomType.CAMERA1.getCode());
                break;
            case R.id.share_camera2:
                sendRequest(RoomType.CAMERA2.getCode());
                break;
            default:
                break;
        }
    }


    @Override
    public void socketCallback(Message message) {
        switch (message.getMessageType()){
            case SOCKET_OPEN:
                break;
            case SOCKET_CLOSE:
                break;
            case SOCKET_ERROR:
                break;
            case ROOM_FULL:
                showFullMessage(message);
                break;
            default:
                break;
        }
    }

    private void showFullMessage(Message message){
        BaseMessage<Room,Object> baseMessage = message.transForm(new BaseMessage<Room, Object>() {});
        String msg = "房间已满，房间号： " + baseMessage.getMessage().getRoomId() + " maxSize: " + baseMessage.getMessage().maxSize() + " currentSize: " + baseMessage.getMessage().currentSize();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void setLocalStream(MediaStream localStream, String selfId) {

    }

    @Override
    public void addRemoteStream(MediaStream localStream, String selfId) {

    }

    @Override
    public void closeWindow(String socketId) {

    }

    //发起请求，必须指定房间号和房间类型，如果房间不存在服务器会主动创建一个房间
    //注意这个方法仅仅是与服务器建立连接而已，成功后再做后续的操作，这里传递房间号和房间类型是为了保存信息
    private void sendRequest(int roomTypeCode){
        LogUtil.d("send connect request");
        WebRtcInterface webRtcInterface = WebRtcManager.getInstance(this,null);
        webRtcInterface.chatRequest(RoomType.getRooType(roomTypeCode),"12345678");
    }


}
