# ImageLoader
[![Download](https://api.bintray.com/packages/yuriy-budiyev/maven/image-loader/images/download.svg)](https://bintray.com/yuriy-budiyev/maven/image-loader/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Image%20Loader-blue.svg?style=flat)](https://android-arsenal.com/details/1/6378)
[![API](https://img.shields.io/badge/API-14%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=14)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7ecfc5f4065c41ba9cd2e9409d072ebb)](https://www.codacy.com/app/yuriy-budiyev/image-loader?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=yuriy-budiyev/image-loader&amp;utm_campaign=Badge_Grade)

Image loader library for Android.
Allows you to build your own image loader that will do exactly what you want.
Almost unlimited customization.

### Usage
```gradle
dependencies {
    implementation 'com.budiyev.android:image-loader:1.9.8'
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
                /*Create new load request, optionally, specify custom bitmap loader*/
                .request()
                /*Select source data type*/
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
