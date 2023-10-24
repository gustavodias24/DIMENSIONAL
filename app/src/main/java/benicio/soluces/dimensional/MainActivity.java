package benicio.soluces.dimensional;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.squareup.picasso.Picasso;

import java.util.concurrent.ExecutionException;

import benicio.soluces.dimensional.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int ACRESCENTADOR = 0;
    private int RAIO_IMAGE = 0;
    private int TAMANHO_INICIAL = 0;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private float maxZoomLevel = 1f; // Variável para armazenar o zoom máximo

    private float currentZoomLevel = 1f;
    private Camera mCamera;
    ImageView rowRed, rowYelow;
    private ActivityMainBinding binding;
    private static final int PERMISSIONS_GERAL = 1;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private PreviewView previewView;
    private  final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if ( result ){
                startCamera(cameraFacing);
            }
        }
    });

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        previewView = binding.cameraPreview;

        rowRed = binding.rowredview;
        rowYelow = binding.rowyelowview;

        binding.maisyelow.setOnClickListener(this);
        binding.menosyelow.setOnClickListener(this);
        binding.maisred.setOnClickListener(this);
        binding.menosred.setOnClickListener(this);
        binding.maisZoom.setOnClickListener(this);
        binding.menosZoom.setOnClickListener(this);
        binding.configuracoes.setOnClickListener(this);

        configurarEventoDePressionar();
        pegarZoomMaximo();

        if (
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera(cameraFacing);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA }, PERMISSIONS_GERAL);
        }

        calcularTamanhoDaTela();
        binding.raioImage.getLayoutParams().width = RAIO_IMAGE;
        binding.raioImage.getLayoutParams().height = RAIO_IMAGE;
        binding.rowredview.getLayoutParams().width = TAMANHO_INICIAL;
        binding.rowyelowview.getLayoutParams().width = TAMANHO_INICIAL;
    }
    public void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();

                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.unbindAll();

                if ( !this.isDestroyed() ){
                    mCamera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                }

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_GERAL) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            // Se todas as permissões foram concedidas, inicie as operações que requerem permissões
            if (allPermissionsGranted) {
                startCamera(cameraFacing);
            } else {
                // Se o usuário recusar alguma permissão, exiba uma mensagem informando a necessidade das permissões
                Toast.makeText(this, "PERMISSÃO NEGADA", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if ( id == binding.maisred.getId() ){
            if ( rowRed.getLayoutParams().width < 360){
                rowRed.getLayoutParams().width = rowRed.getWidth() + ACRESCENTADOR;
                rowRed.requestLayout();

                Log.d("rowstest", "onClick: " + (binding.rowredview.getWidth() + ACRESCENTADOR) );
            }

        }else if (id == binding.menosred.getId()){
            if ( rowRed.getWidth() > 40){
                rowRed.getLayoutParams().width = rowRed.getWidth() - ACRESCENTADOR;
                rowRed.requestLayout();

                Log.d("rowstest", "onClick: " + (rowRed.getWidth() - ACRESCENTADOR) );
            }

        }else if (id == binding.maisyelow.getId()){
            if ( rowYelow.getLayoutParams().width < 360){
                rowYelow.getLayoutParams().width = rowYelow.getWidth() + ACRESCENTADOR;
                rowYelow.requestLayout();

                Log.d("rowstest", "onClick: " + (rowYelow.getWidth() + ACRESCENTADOR) );
            }

        }else if (id == binding.menosyelow.getId()){
            if ( rowYelow.getWidth() > 40){
                rowYelow.getLayoutParams().width = rowYelow.getWidth() - ACRESCENTADOR;
                rowYelow.requestLayout();

                Log.d("rowstest", "onClick: " + (rowYelow.getWidth() - ACRESCENTADOR) );
            }
        }else if ( id == binding.configuracoes.getId() ){
            startActivity(new Intent(getApplicationContext(), ConfiguracoesActivity.class));
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public void configurarEventoDePressionar(){

        binding.maisZoom.setOnTouchListener((view, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                // Quando o botão é pressionado
                if ( currentZoomLevel < maxZoomLevel ){
                    currentZoomLevel += 0.5f;
                    mCamera.getCameraControl().setZoomRatio(currentZoomLevel);

                    String zoomString = currentZoomLevel  + "x";
                    binding.textViewZoom.setText(zoomString);
                    Toast.makeText(this, zoomString, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        binding.menosZoom.setOnTouchListener((view, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                // Quando o botão é pressionado
                if ( currentZoomLevel > 1){
                    currentZoomLevel -= 0.5f;
                    mCamera.getCameraControl().setZoomRatio(currentZoomLevel);

                    String zoomString = currentZoomLevel  + "x";
                    binding.textViewZoom.setText(zoomString);
                    Toast.makeText(this, zoomString, Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });
    }

    public void pegarZoomMaximo(){
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Selecionar a câmera traseira como padrão
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Configurar o Preview da câmera
                Preview preview = new Preview.Builder().build();

                // Configurar o ImageCapture da câmera
                ImageCapture imageCapture = new ImageCapture.Builder().build();

                // Vincular a câmera ao ciclo de vida
                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);

                // Obter o zoom máximo da câmera usando Camera2 API
                CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
                String cameraId = cameraManager.getCameraIdList()[0];
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                float maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

                if (maxZoom > 1) {
                    maxZoomLevel = maxZoom;
                }

            } catch (Exception e) {
                // Lidar com exceções relacionadas à câmera
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("DefaultLocale")
    public void calcularTamanhoDaTela(){
        // Obtendo as dimensões da tela em pixels
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int heightPixels = displayMetrics.heightPixels;
        int widthPixels = displayMetrics.widthPixels;

        // Calculando as dimensões em centímetros
        double heightInches = heightPixels / displayMetrics.ydpi;
        double widthInches = widthPixels / displayMetrics.xdpi;

        // Convertendo polegadas para centímetros (1 polegada = 2.54 cm)
        double heightCm = heightInches * 2.54;
        double widthCm = widthInches * 2.54;

        TAMANHO_INICIAL = (int) Math.ceil((200 * widthCm) / 13.78);
        RAIO_IMAGE = (int) Math.ceil((60 * widthCm) / 13.78);
        ACRESCENTADOR = (int) Math.ceil(((40 * widthCm) / 13.78));


        Log.d("calcularTamanhoDaTela", "acrescenteador: " + ACRESCENTADOR);
        Log.d("calcularTamanhoDaTela", "raio: " + RAIO_IMAGE);
        Log.d("calcularTamanhoDaTela", "tamanho inicial: " + TAMANHO_INICIAL);
        binding.dadosDaTela.setText(
                String.format(
                        "Altura: %.2f cm"
                        +"\n"+
                        "Largura: %.2f cm"
                        +"\n"+
                        "Altura: %.2f polegadas"+
                        "\n"+
                        "Largura: %.2f polegadas"
                        , heightCm, widthCm, heightInches, widthInches)
        );

    }
}