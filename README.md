# Image Loader
[![Download](https://api.bintray.com/packages/yuriy-budiyev/maven/image-loader/images/download.svg)](https://bintray.com/yuriy-budiyev/maven/image-loader/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Image%20Loader-blue.svg?style=flat)](https://android-arsenal.com/details/1/6378)
[![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7ecfc5f4065c41ba9cd2e9409d072ebb)](https://www.codacy.com/app/yuriy-budiyev/image-loader?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=yuriy-budiyev/image-loader&amp;utm_campaign=Badge_Grade)

Image loader library for Android.

### Features
* Image transformations
* Automatic memory and storage caching
* Ability to load images from any custom data type
* Both synchronous and asynchronous image loading modes
* Almost unlimited customization

### Usage ([sample](https://github.com/yuriy-budiyev/lib-demo-app))
Add dependency:
```gradle
dependencies {
    implementation 'com.budiyev.android:image-loader:2.5.6'
}
```
And load images simply:
```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView view = findViewById(R.id.image_view);

        // Simply load image from URL into view
        ImageLoader.with(this).from("https://some.url/image").load(view);

        // Advanced usage
        ImageLoader.with(this)
                /*Create new load request for specified data.
                  Data types supported by default: URIs (remote and local), 
                  files, file descriptors, resources and byte arrays.*/
                .from("https://some.url/image")
                /*Required image size (to load sampled bitmaps)*/
                .size(1000, 1000)
                /*Display loaded image with rounded corners, optionally, specify corner radius*/
                .roundCorners()
                /*Placeholder drawable*/
                .placeholder(new ColorDrawable(Color.LTGRAY))
                /*Error drawable*/
                .errorDrawable(new ColorDrawable(Color.RED))
                /*Apply transformations*/
                .transform(ImageUtils.cropCenter())
                .transform(ImageUtils.convertToGrayScale())
                /*Load image into view*/
                .load(view);
                /*Also, load, error and display callbacks can be specified for each request*/

        // Load image asynchronously without displaying it
        ImageLoader.with(this).from("https://some.url/image").onLoaded(new LoadCallback() {
            @Override
            public void onLoaded(@NonNull Bitmap image) {
                // Do something with image here
            }
        }).load();

        // Load image synchronously (on current thread), should be executed on a worker thread
        //Bitmap image = ImageLoader.with(this).from("https://some.url/image").loadSync();                 
    }
}
```
