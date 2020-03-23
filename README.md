
# react-native-ziti

## Getting started

`$ npm install react-native-ziti --save`

### Mostly automatic installation

`$ react-native link react-native-ziti`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-ziti` and add `RNZiti.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNZiti.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import io.netfoundry.ziti.rn.RNZitiPackage;` to the imports at the top of the file
  - Add `new RNZitiPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-ziti'
  	project(':react-native-ziti').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-ziti/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-ziti')
  	```


## Usage
```javascript
import RNZiti from 'react-native-ziti';

// TODO: What to do with the module?
RNZiti;
```
  