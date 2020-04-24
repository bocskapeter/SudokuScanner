package eu.bopet.sudokuscanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SudokuScanner";
    private static final int LINE_WIDTH = 2;
    private static final int LINE_WIDTH_THICK = 8;
    private static final int GRIDS = 9;
    private static final int MAIN_GRIDS = 3;
    private static final int MARGIN = 5;
    private static final List<String> VALUES = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9");
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private TextureView textureView;
    private SurfaceView surfaceView;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private int square;
    private int margin;
    private SurfaceHolder holder;
    private Canvas canvas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(textureListener);
        ImageButton takePictureButton = findViewById(R.id.imgCapture);
        takePictureButton.setOnClickListener(v -> takePicture());

        surfaceView = findViewById(R.id.surface_view);
        surfaceView.setZOrderOnTop(true);
        SurfaceHolder mHolder = surfaceView.getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                holder = surfaceHolder;
                canvas = surfaceHolder.lockCanvas();
                if (canvas == null) {
                    Log.e(TAG, "Cannot draw onto the canvas as it's null");
                } else {
                    Paint myPaint = new Paint();
                    myPaint.setColor(Color.rgb(60, 20, 100));
                    myPaint.setStrokeWidth(LINE_WIDTH);
                    myPaint.setStyle(Paint.Style.STROKE);

                    Paint thickPaint = new Paint();
                    thickPaint.setColor(Color.rgb(30, 10, 50));
                    thickPaint.setStrokeWidth(LINE_WIDTH_THICK);
                    thickPaint.setStyle(Paint.Style.STROKE);

                    int sideLength;
                    if (surfaceView.getWidth() > surfaceView.getHeight()) {
                        sideLength = surfaceView.getHeight();
                    } else {
                        sideLength = surfaceView.getWidth();
                    }
                    square = (sideLength - (2 * MARGIN)) / GRIDS;
                    margin = ((sideLength - (square * GRIDS)) / 2);

                    int mainSquare = square * (GRIDS / MAIN_GRIDS);

                    drawGrids(GRIDS, canvas, margin, square, myPaint);
                    drawGrids(MAIN_GRIDS, canvas, margin, mainSquare, thickPaint);

                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });

    }

    private void drawGrids(int grids, Canvas canvas, int margin, int square, Paint myPaint) {
        for (int i = 0; i < grids; i++) {
            for (int j = 0; j < grids; j++) {
                canvas.drawRect(margin + i * square, margin + j * square, margin + i * square + square, margin + j * square + square, myPaint);
            }
        }
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };


    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void takePicture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        try {
            ImageReader reader = ImageReader.newInstance(imageDimension.getWidth(), imageDimension.getHeight(), ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            file = new File(Environment.getExternalStorageDirectory() + "/" + getDate() + ".jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    try (Image image = reader.acquireLatestImage()) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        new Thread(() -> {
                            Log.e(TAG, "thread started...");
                            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                            Log.e(TAG, "scale bitmap... " + bitmapImage.getWidth() + "x" + bitmapImage.getHeight());
                            float ratio = (float) bitmapImage.getWidth() / (float) bitmapImage.getHeight();
                            int width = 640;
                            int height = (int) (width / ratio);
                            bitmapImage = Bitmap.createScaledBitmap(bitmapImage, width, height, true);
                            Log.e(TAG, "convert to grayscale... " + bitmapImage.getWidth() + "x" + bitmapImage.getHeight());
                            bitmapImage = toGrayscale(bitmapImage);
                            Log.e(TAG, "split bitmap... " + bitmapImage.getWidth() + "x" + bitmapImage.getHeight());
                            Bitmap[][] squareBitmaps = splitBitmap(bitmapImage);
                            Log.e(TAG, "get numbers...");
                            int[][] numbers = getNumbersFromBitmaps(squareBitmaps);
                            int[][] solution = solveSudoku(numbers);
                            Log.e(TAG, "draw numbers...");
                            drawNumbers(solution);
                        }).start();
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    try (OutputStream output = new FileOutputStream(file)) {
                        output.write(bytes);
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int[][] solveSudoku(int[][] numbers) {
        int[][] solution = new int[numbers.length][numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            for (int j = 0; j < numbers.length; j++) {
                solution[i][j] = numbers[i][j];
            }
        }
        int nol = 0;
        int oldNol = numbers.length * numbers.length;
        boolean converged = true;
        List<Integer>[][] pn = new ArrayList[solution.length][solution.length];
        int bx, by;

        for (int i = 0; i < solution.length; i++) {
            for (int j = 0; j < solution.length; j++) {
                if (solution[i][j] == 0) {
                    pn[i][j] = new ArrayList<>();
                    for (int k = 1; k < solution.length + 1; k++) {
                        pn[i][j].add(k);
                    }
                }
            }
        }
        while (converged) {
            for (int i = 0; i < solution.length; i++) {
                for (int j = 0; j < solution.length; j++) {
                    if (solution[i][j] == 0) {
                        //check column
                        for (int k = 0; k < solution.length; k++) {
                            if (solution[i][k] > 0 && pn[i][j].contains(solution[i][k])) {
                                pn[i][j].remove((Integer) solution[i][k]);
                            }
                        }
                        //check row
                        for (int k = 0; k < solution.length; k++) {
                            if (solution[k][j] > 0 && pn[i][j].contains(solution[k][j])) {
                                pn[i][j].remove((Integer) solution[k][j]);
                            }
                        }
                        //check block
                        for (int k = 0; k < MAIN_GRIDS; k++) {
                            for (int l = 0; l < MAIN_GRIDS; l++) {
                                bx = (i / MAIN_GRIDS + k);
                                by = (j / MAIN_GRIDS + l);
                                if (solution[i][j] > 0 && pn[bx][by].contains(solution[i][j])) {
                                    pn[bx][by].remove((Integer) solution[i][j]);
                                }
                            }
                        }
                        if (pn[i][j].size() == 1) {
                            solution[i][j] = pn[i][j].get(0);
                            pn[i][j] = null;
                        } else {
                            nol++;
                        }
                    }
                }
            }
            if (oldNol > nol) {
                converged = true;
                oldNol = nol;
                nol = 0;
            } else {
                converged = false;
            }
        }
        return solution;
    }

    private String getDate() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        return format.format(new Date());
    }

    private Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private void drawNumbers(int[][] numbers) {
        canvas = holder.lockCanvas();
        for (int i = 0; i < numbers.length; i++) {
            for (int j = 0; j < numbers.length; j++) {
                if (numbers[i][j]==0) continue;
                Paint myPaint = new Paint();
                myPaint.setColor(Color.rgb(150, 20, 100));
                int hSqr = square / 2;
                myPaint.setTextSize(hSqr);
                myPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(String.valueOf(numbers[i][j]), margin + i * square + hSqr, margin + j * square + hSqr, myPaint);
            }
        }
        holder.unlockCanvasAndPost(canvas);
    }

    private int[][] getNumbersFromBitmaps(Bitmap[][] bitmaps) {
        int[][] result = new int[bitmaps.length][bitmaps.length];
        Log.e(TAG, "Create Recogniser: ");
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        if (!textRecognizer.isOperational()) {
            Log.e(TAG, "Detector dependencies are not yet available.");
        }
        Frame[][] frames = new Frame[bitmaps.length][bitmaps.length];
        for (int i = 0; i < bitmaps.length; i++) {
            for (int j = 0; j < bitmaps.length; j++) {
                frames[i][j] = new Frame.Builder()
                        .setBitmap(bitmaps[i][j])
                        .build();
            }
        }
        SparseArray<TextBlock>[][] blocks = new SparseArray[bitmaps.length][bitmaps.length];
        Log.e(TAG, "Iterate images: ");
        for (int i = 0; i < bitmaps.length; i++) {
            for (int j = 0; j < bitmaps.length; j++) {
                blocks[i][j] = textRecognizer.detect(frames[i][j]);
                if (blocks[i][j].size() == 1) {
                    String text = blocks[i][j].get(0).getValue();
                    if (text.length() == 1 && VALUES.contains(text)) {
                        result[i][j] = Integer.parseInt(text);
                    }
                }
            }
        }
        return result;
    }

    private Bitmap[][] splitBitmap(Bitmap bitmapImage) {
        int sqr;
        if (bitmapImage.getWidth() > bitmapImage.getHeight()) {
            sqr = (int) ((float) bitmapImage.getHeight() / (float) GRIDS);
        } else {
            sqr = (int) ((float) bitmapImage.getWidth() / (float) GRIDS);
        }
        Log.e(TAG, "split image of: " + bitmapImage.getWidth() + "x" + bitmapImage.getHeight() + " to squares: " + sqr);
        Bitmap[][] bitmaps = new Bitmap[GRIDS][GRIDS];
        for (int x = 0; x < GRIDS; ++x) {
            for (int y = 0; y < GRIDS; ++y) {
                bitmaps[x][y] = Bitmap.createBitmap(bitmapImage, x * sqr, y * sqr, sqr, sqr);
            }
        }
        return bitmaps;
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            assert manager != null;
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            Size[] availableSizes = map.getOutputSizes(SurfaceTexture.class);
            Log.e(TAG, "Supported image sizes: ");
            imageDimension = availableSizes[0];
            for (Size s : availableSizes) {
                if (s.getWidth() == 1280) {
                    imageDimension = s;
                }
                Log.e(TAG, " --> " + s.getWidth() + " x " + s.getHeight());
            }

            float ratioCamera = (float) imageDimension.getWidth() / (float) imageDimension.getHeight();

            Log.e(TAG, "Image size: " + imageDimension.getWidth() + " x " + imageDimension.getHeight() + " ratio: " + ratioCamera);
            Log.e(TAG, "View size: " + textureView.getWidth() + " x " + textureView.getHeight());

            int width;
            int height;

            float scaleX = (float) imageDimension.getHeight() / (float) textureView.getWidth();

            width = textureView.getWidth();
            height = (int) (imageDimension.getWidth() / scaleX);

            Log.e(TAG, "Set to size: " + width + " x " + height);

            textureView.setLayoutParams(new FrameLayout.LayoutParams(width, height));


            Log.e(TAG, "After: ");
            Log.e(TAG, "Image size: " + imageDimension.getWidth() + " x " + imageDimension.getHeight());
            Log.e(TAG, "View size: " + textureView.getLayoutParams().width + " x " + textureView.getLayoutParams().height);


            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}
