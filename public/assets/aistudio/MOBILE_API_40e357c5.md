# Mobile App API Guide

এই ডকুমেন্টে Android/iOS/Flutter অ্যাপ থেকে ভিডিও তথ্য, resolution list এবং download link ব্যবহারের ধাপ দেখানো হয়েছে।

## Base URL

```text
https://ytdlp-production-4d018ba3.up.railway.app
```

সব endpoint-এ HTTPS ব্যবহার করুন। কোনো API key header দরকার নেই। Request body JSON এবং header `Content-Type: application/json` হবে।

## API flow

1. ব্যবহারকারীর YouTube URL যাচাই করে `/video_info` দিয়ে title ও metadata নিন।
2. `/available_resolutions` দিয়ে ঐ ভিডিওতে বাস্তবে পাওয়া quality দেখান। তালিকা hardcode করবেন না—কোনো ভিডিওতে API শুধু `360p` দিতে পারে।
3. ব্যবহারকারী quality বেছে নিলে `/download/{quality}`-এ request পাঠান; যেমন `/download/360p`।
4. Request চলাকালে indeterminate loading দেখান। এই API-তে progress polling endpoint নেই।
5. Response-এর `download_url` খুলুন বা সেটি native download manager দিয়ে ডাউনলোড করুন। `selected.height` থাকলে selected quality-এর সঙ্গে মেলান।

## Endpoints

### `POST /video_info`

Request:

```json
{
  "url": "https://www.youtube.com/watch?v=JOTYKhp9Rk0"
}
```

Response-এ সাধারণত `title`, `author` বা `uploader`, `length` বা `duration`, এবং `views` বা `view_count` থাকে। উপলভ্য হলে `thumbnail`-ও থাকতে পারে। কোনো field না থাকলে উপযুক্ত fallback দেখান।

### `POST /available_resolutions`

একই JSON request body ব্যবহার করুন। Resolution response object-এ `all`, `resolutions` বা `progressive` নামে list থাকতে পারে; canonical উদাহরণ:

```json
{
  "all": ["144p", "240p", "360p", "720p"]
}
```

উদাহরণের তালিকা শুধু format বোঝানোর জন্য—প্রতিটি ভিডিওতে সব quality পাওয়া যাবে না। List ফাঁকা হলে download option বন্ধ রাখুন এবং API-এর error দেখান।

### `POST /download/{quality}`

Quality-টি resolution list থেকে নিন এবং JSON request body-তে URL পাঠান:

```http
POST /download/360p
Content-Type: application/json
```

```json
{
  "url": "https://www.youtube.com/watch?v=JOTYKhp9Rk0"
}
```

JSON response-এর উদাহরণ:

```json
{
  "download_url": "https://download-host.example/video.mp4",
  "selected": {
    "height": 360
  }
}
```

`download_url`-ই আসল download link। `selected.height` response-এ থাকলে requested quality-এর সঙ্গে না মিললে ভুল quality-র download link ব্যবহার করবেন না; error দেখান। পুরোনো server binary video response দিতে পারে—সেক্ষেত্রে response-টি bytes হিসেবে save করুন, JSON হিসেবে parse করবেন না।

## Flutter / Dart example

`http` package যোগ করুন (`flutter pub add http`)। নিচের service-টি JSON API response-এর জন্য:

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

const apiBase = 'https://ytdlp-production-4d018ba3.up.railway.app';

class VideoApi {
  Future<Map<String, dynamic>> _post(String path, String videoUrl) async {
    final response = await http
        .post(
          Uri.parse('$apiBase$path'),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({'url': videoUrl}),
        )
        .timeout(const Duration(seconds: 90));

    dynamic decoded;
    try {
      decoded = jsonDecode(response.body);
    } on FormatException {
      throw Exception('API JSON response দেয়নি (HTTP ${response.statusCode})');
    }

    if (response.statusCode < 200 || response.statusCode >= 300) {
      final message = decoded is Map
          ? decoded['error'] ?? decoded['message']
          : null;
      throw Exception(message?.toString() ?? 'Request failed (${response.statusCode})');
    }
    if (decoded is! Map<String, dynamic>) {
      throw Exception('API response format সঠিক নয়');
    }
    return decoded;
  }

  Future<Map<String, dynamic>> videoInfo(String videoUrl) =>
      _post('/video_info', videoUrl);

  Future<List<String>> availableResolutions(String videoUrl) async {
    final data = await _post('/available_resolutions', videoUrl);
    final values = data['all'] ?? data['resolutions'] ?? data['progressive'];
    if (values is! List) return [];
    return values.map((value) => value.toString()).toList();
  }

  Future<Map<String, dynamic>> generateDownload(
    String videoUrl,
    String quality,
  ) =>
      _post('/download/${Uri.encodeComponent(quality)}', videoUrl);
}
```

ব্যবহারের ক্রম:

```dart
final api = VideoApi();
final info = await api.videoInfo(videoUrl);
final qualities = await api.availableResolutions(videoUrl);

// UI-তে qualities দেখান, তারপর user-এর নির্বাচিত মান পাঠান।
final result = await api.generateDownload(videoUrl, '360p');
final downloadUrl = result['download_url'] as String;
```

`downloadUrl`-কে external browser-এ খোলা যায়, অথবা `dio`/platform download manager দিয়ে app-এর Downloads folder-এ save করা যায়। `selected.height` ফেরত এলে নির্বাচিত quality-এর সঙ্গে যাচাই করুন।

## Mobile implementation notes

- Android-এ `AndroidManifest.xml`-এ `android.permission.INTERNET` থাকতে হবে।
- Native Android/iOS অ্যাপে browser CORS নিয়ম প্রযোজ্য নয়; Flutter Web-এ CORS server configuration লাগতে পারে।
- Network timeout, offline অবস্থা, non-2xx status এবং JSON-এর `error`/`message` field handle করুন। Request চলাকালে loading দেখান এবং শেষে UI state ফিরিয়ে আনুন।
- `download_url` যদি relative path হয়, API base URL-এর সঙ্গে resolve করুন।
- Download link-কে স্থায়ী ধরে রাখবেন না; response পাওয়ার পর দ্রুত ব্যবহার করুন।
