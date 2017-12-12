# Image Loader
[![Download](https://api.bintray.com/packages/yuriy-budiyev/maven/image-loader/images/download.svg)](https://bintray.com/yuriy-budiyev/maven/image-loader/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Image%20Loader-blue.svg?style=flat)](https://android-arsenal.com/details/1/6378)
[![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7ecfc5f4065c41ba9cd2e9409d072ebb)](https://www.codacy.com/app/yuriy-budiyev/image-loader?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=yuriy-budiyev/image-loader&amp;utm_campaign=Badge_Grade)

Image loader library for Android.

### Features
* Image transformations
* Automatic memory and storage caching
* Almost unlimited customization
* Generic load requests, ability to load images from any custom data type
* Both synchronous and asynchronous image loading modes

### Usage
```gradle
dependencies {
    implementation 'com.budiyev.android:image-loader:2.0.1'
}
```
### Basic usage sample
Basic implementation automatically cares about memory and storage caching

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView view = findViewById(R.id.image_view);

        ImageLoader.with(this)
                /*Create new load request,
                  optionally, specify custom bitmap loader to be able to
                  load bitmaps from any data type you need*/
                .request()
                /*Select source data type (if custom bitmap loader isn't specified),
                  supported: Android URIs, string URLs, files, file descriptors,
                  resources and byte arrays*/
                .url()
                /*Set source data*/
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
    }
}
```
