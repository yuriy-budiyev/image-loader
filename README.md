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
    implementation 'com.budiyev.android:image-loader:1.6.8'
}
```

### Simple singleton implementation
```java
/**
 * Simple image loader that automatically cares about memory and storage caching,
 * read documentation for more info
 */
public final class MyImageLoader {
    private static volatile MyImageLoader sInstance;
    private final ImageLoader<Uri> mLoader;

    private MyImageLoader(@NonNull Context context) {
        mLoader = ImageLoader.builder(context).uri().memoryCache().storageCache().build();
    }

    /**
     * Load image form {@code url} to {@code view}
     *
     * @param url  Source URL
     * @param view Target image view
     */
    @MainThread
    public void load(@NonNull String url, @NonNull ImageView view) {
        mLoader.load(Uri.parse(url), view);
    }

    /**
     * Get (or create, if not exist) loader instance
     *
     * @param context Context
     * @return Loader instance
     */
    @NonNull
    public static MyImageLoader with(@NonNull Context context) {
        MyImageLoader instance = sInstance;
        if (instance == null) {
            synchronized (MyImageLoader.class) {
                instance = sInstance;
                if (instance == null) {
                    instance = new MyImageLoader(context);
                    sInstance = instance;
                }
            }
        }
        return instance;
    }

    /**
     * Clear memory cache if it exists, would be good to call this method in
     * {@link Application#onTrimMemory(int)} method,
     * read documentation for more info
     */
    public static void clearMemoryCache() {
        MyImageLoader instance = sInstance;
        if (instance != null) {
            instance.mLoader.clearMemoryCache();
        }
    }
}
```
### Usage sample
```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView view = findViewById(R.id.image_view);
        
        MyImageLoader.with(this).load("https://some.url/image", view);
    }
}
```
