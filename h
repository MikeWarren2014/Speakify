[1mdiff --git a/app/src/main/AndroidManifest.xml b/app/src/main/AndroidManifest.xml[m
[1mindex 26d4d6c..0a8b645 100644[m
[1m--- a/app/src/main/AndroidManifest.xml[m
[1m+++ b/app/src/main/AndroidManifest.xml[m
[36m@@ -37,6 +37,9 @@[m
         android:supportsRtl="true"[m
         android:theme="@style/Theme.MyApplication"[m
         tools:targetApi="31" >[m
[32m+[m[32m        <meta-data[m
[32m+[m[32m            android:name="com.google.android.gms.version"[m
[32m+[m[32m            android:value="@integer/google_play_services_version" />[m
         <activity[m
             android:name=".activities.LoginActivity"[m
             android:exported="true"[m
[1mdiff --git a/app/src/main/java/com/mikewarren/speakify/activities/NotificationPermissionsActivity.kt b/app/src/main/java/com/mikewarren/speakify/activities/NotificationPermissionsActivity.kt[m
[1mindex 5fc07da..a0538c6 100644[m
[1m--- a/app/src/main/java/com/mikewarren/speakify/activities/NotificationPermissionsActivity.kt[m
[1m+++ b/app/src/main/java/com/mikewarren/speakify/activities/NotificationPermissionsActivity.kt[m
[36m@@ -3,13 +3,11 @@[m [mpackage com.mikewarren.speakify.activities[m
 import android.Manifest[m
 import android.app.AlertDialog[m
 import android.content.Intent[m
[31m-import android.content.pm.PackageManager[m
 import android.os.Build[m
 import android.os.Bundle[m
 import android.provider.Settings[m
 import android.util.Log[m
 import androidx.activity.result.contract.ActivityResultContracts[m
[31m-import androidx.core.content.ContextCompat[m
 import com.mikewarren.speakify.R[m
 import com.mikewarren.speakify.data.constants.PermissionCodes[m
 import com.mikewarren.speakify.data.events.NotificationPermissionEvent[m
[36m@@ -22,6 +20,8 @@[m [mclass NotificationPermissionsActivity :[m
         permissionRequestCode = PermissionCodes.NotificationPermissions[m
     ) {[m
 [m
[32m+[m[32m    private var activeDialog: AlertDialog? = null[m
[32m+[m
     private val requestMultiplePermissionsLauncher =[m
         registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->[m
             // Check POST_NOTIFICATIONS (Android 13+)[m
[36m@@ -41,32 +41,13 @@[m [mclass NotificationPermissionsActivity :[m
 [m
     override fun onCreate(savedInstanceState: Bundle?) {[m
         super.onCreate(savedInstanceState)[m
[31m-        // The Base class logic for requestPermissions() is slightly different here[m
[31m-        // because we have a mix of Runtime + System Settings permissions.[m
[31m-        startPermissionFlow()[m
[32m+[m[32m        // The Base class calls requestPermissions() in its onCreate(),[m
[32m+[m[32m        // which will trigger the flow via getPermissions() and handleUngrantedPermissions().[m
     }[m
 [m
[31m-    private fun startPermissionFlow() {[m
[31m-        val permissionsToRequest = getPermissions()[m
[31m-[m
[31m-        // if we are on Android 12 or below, we just check the listener permission[m
[31m-        if (permissionsToRequest.isEmpty()) {[m
[31m-            checkListenerPermission()[m
[31m-            return[m
[31m-        }[m
[31m-[m
[31m-        // If we have runtime permissions to request (Android 13+), do that first[m
[31m-        val ungranted = permissionsToRequest.filter {[m
[31m-            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED[m
[31m-        }.toTypedArray()[m
[31m-[m
[31m-        if (ungranted.isEmpty()) {[m
[31m-            // Runtime permissions already granted, check listener[m
[31m-            checkListenerPermission()[m
[31m-            return[m
[31m-        }[m
[31m-[m
[31m-        requestMultiplePermissionsLauncher.launch(ungranted)[m
[32m+[m[32m    override fun onDestroy() {[m
[32m+[m[32m        activeDialog?.dismiss()[m
[32m+[m[32m        super.onDestroy()[m
     }[m
 [m
     override fun getPermissions(): Array<String> {[m
[36m@@ -81,11 +62,11 @@[m [mclass NotificationPermissionsActivity :[m
             showListenerPermissionDialog()[m
             return[m
         }[m
[31m-        onPermissionGranted()[m
[32m+[m[32m        allPermissionsGranted()[m
     }[m
 [m
     private fun showListenerPermissionDialog() {[m
[31m-        AlertDialog.Builder(this)[m
[32m+[m[32m        activeDialog = AlertDialog.Builder(this)[m
             .setTitle(getString(R.string.notification_access_required_title))[m
             .setMessage(getString(R.string.notification_access_required_message))[m
             .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->[m
[36m@@ -109,7 +90,7 @@[m [mclass NotificationPermissionsActivity :[m
             return[m
         }[m
         if (NotificationPermissionHelper(this).isNotificationServiceEnabled()) {[m
[31m-            onPermissionGranted()[m
[32m+[m[32m            allPermissionsGranted()[m
             return[m
         }[m
         // User came back without enabling it[m
[36m@@ -118,6 +99,10 @@[m [mclass NotificationPermissionsActivity :[m
     }[m
 [m
     override fun onPermissionGranted() {[m
[32m+[m[32m        checkListenerPermission()[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    private fun allPermissionsGranted() {[m
         Log.d(this.javaClass.name, "All notification permissions granted.")[m
         eventBus.post(NotificationPermissionEvent.PermissionGranted)[m
         finish()[m
[36m@@ -133,8 +118,7 @@[m [mclass NotificationPermissionsActivity :[m
 [m
     override fun handleUngrantedPermissions(ungrantedPermissions: Array<String>) {[m
         // This is called by BasePermissionRequester if the initial runtime request fails[m
[31m-        // or if we manually trigger it.[m
[31m-        AlertDialog.Builder(this)[m
[32m+[m[32m        activeDialog = AlertDialog.Builder(this)[m
             .setTitle(getString(R.string.permissions_required_title))[m
             .setMessage(getString(R.string.permissions_required_message))[m
             .setPositiveButton(getString(R.string.ok)) { _, _ ->[m
[1mdiff --git a/app/src/main/java/com/mikewarren/speakify/strategies/GeohNotificationStrategy.kt b/app/src/main/java/com/mikewarren/speakify/strategies/GeohNotificationStrategy.kt[m
[1mindex ce18a24..2158dd5 100644[m
[1m--- a/app/src/main/java/com/mikewarren/speakify/strategies/GeohNotificationStrategy.kt[m
[1m+++ b/app/src/main/java/com/mikewarren/speakify/strategies/GeohNotificationStrategy.kt[m
[36m@@ -68,7 +68,17 @@[m [mITaggable {[m
 [m
         return TimeUtils.ExtractRelativeTime(timeString,[m
             onGetDateTime = { hhMM ->[m
[31m-                TimeUtils.GetLocalDateTimeFrom(DayOfWeek.FRIDAY, hhMM)[m
[32m+[m[32m                val dayOfWeek: DayOfWeek = when (matchResult.groups["dayOfWeek"]?.value) {[m
[32m+[m[32m                    "Mon" -> DayOfWeek.MONDAY[m
[32m+[m[32m                    "Tue" -> DayOfWeek.TUESDAY[m
[32m+[m[32m                    "Wed" -> DayOfWeek.WEDNESDAY[m
[32m+[m[32m                    "Thu" -> DayOfWeek.THURSDAY[m
[32m+[m[32m                    "Fri" -> DayOfWeek.FRIDAY[m
[32m+[m[32m                    "Sat" -> DayOfWeek.SATURDAY[m
[32m+[m[32m                    "Sun" -> DayOfWeek.SUNDAY[m
[32m+[m[32m                    else -> return@ExtractRelativeTime null[m
[32m+[m[32m                }[m
[32m+[m[32m                TimeUtils.GetLocalDateTimeFrom(dayOfWeek, hhMM)[m
             })[m
 [m
     }[m
