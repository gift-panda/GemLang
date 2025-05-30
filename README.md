How to Use:

Download the Interpreter.jar and GemGui.jar and keep in the SAME directory.
Use jdk 9+ to execute the jar.

OR

Run the Interpreter.jar from the terminal, passing the path to a .gem file for execution.

==============================
Syntax:

Declarations:
=================
var name = value; // declares a variable and assigns a value.
//also decomposed into
var name;
name = value;

Loops
=================
for(var i = start; i <= end; i = i + 1){body}
while(condition){body}

{body} // Independent blocks with their own scopes

Functions
==================
func name(param1, param2, ...){
    body
}
// for functions.

//Functions are overloaded by arity
func name(param1){ body }
func name(param1, param2){ body }
//Both are dispatched at runtime to their correct function calls.

//Functions can be binded to any name.
func name(params){body}
var bind = name;
bind(params);

Classes
====================

class className : superClass(){ // className is the child class. superClass is the parent class. 

    var var1 = value; //Treated as public & static fields
    var #var = value; //Treated as private & static fields. 

    static staticFunc(params){ //Treated as static function.
        //Can access private and static fields here
        body
    }

    instFunc(params){ //Treated as instance functions.
        this.var2 = value; //Treated as public & instance fields.
        this.#var3 = value; //Treated as private & instance fields.

        //Can access private and static fields here.
        //Can access private and instance fields here.
        
        body
    }

    init(params){ //Constructor of the class.
                  //Default constructor with no parms created implicitly when explicitly not defined.
                  //Can be private #init() to stop creation of instances.
        body
    }

    toString(){ //Implicitly called by the interpreter when 1. Printing a instance. 2. Added instances to strings. 
                //Default created for all class implicitly is explicitly not defined.
        return string;
    }
}


className.var1; //Accessing static fields
className.staticFunct(params) //Accessing static methods.

this.var2 or inst.var2; //Accessing instance fields.

className(params) //Creating instance of a class. <init>s are overloaded and dispatched too.


Errors
=====================

RuntimeError(String); //The base error class

//Extend your class using RuntimeError to be throw

throw errorInstance; //To throw a error
//or
throw SomeError("message"); //To throw your error
//or
throw RuntimeError("message"); //To throw the default RuntimeError.

try{
    body
}
catch(IDENTIFIER){ //IDENTIFIER is the captured the error.
    body
}
finally{
    body
}

PreBuilt Errors:
1. RuntimeError
2. IllegalArgumentError
3. IllegalArgumentsError
4. IndexOutOfBoundsError
5. NumberFormatError
6. BooleanFormatError

Properties of RuntimeErrors:
getMessage() -> Returns the message passed
getStackTrace() -> Returns the stack trace associated with the object
toString() -> Returns the message shown when thrown


Value Types
=====================
There exist:
1. Number
2. Boolean
3. String
4. List

All are wrapped at time of creation implicitly.
Lists and Strings are special as they support indexing and slicing.

var a = "Some String";
a[num] -> Gives the character present at index num.
a[start:end] -> Gives a substring of the string from index start to end.

var b = [1, 2, 3, 4, 5];
b[num] -> Gives the value present at index num.
b[start:end] -> Gives a list containing the values from index start to end.

Wrapper Classes
======================

Gem doesnt contain any primitive type exposed to the user.
All inbuilt types are wrapped into the following wrapper classes:

1. Number
2. Boolean
3. String
4. List

Number function:
1. init(Number) -> Provided for support. Creates a new Number using existing Number object
2. toString() -> Provided for support. Returns the number.

Boolean functions:
1. init(Boolean) -> Provided for support. Creates a new Boolean using existing Boolean object.
2. toString() -> Provided for support. Returns the boolean.

String functions:
1. init(String) -> Provided for support. Creates a new String using an existing string.
2. parse() -> Parses the string and returns a Boolean, Number, or a String if conversion not possible.
3. parseBoolean() -> Forces a Boolean to be parsed. Throws BooleanFormatError if conversion not possible.
4. parseNumber() -> Forces a Number to be parsed. Throws NumberFormatError if coneversion not possible.
5. isDigit() -> Returns a Boolean by checking if the first character of the given string is a digit.
6. isLetter() -> Returns a Boolean by checking if the first character of the given string is a letter.
7. isAlphanumeric() -> Returns a Boolean by checking if the first character of the given string is either a digit or a letter.
8. toUpperCase() -> Returns a String by changing all the lowercase letters into uppercase.
9. toLowerCase() -> Returns a String by changing all the uppercase letters into lowercase.
10.trims() -> Returns a String by stripping off the leading/trailing white spaces.
11.startsWith(prefix) -> Returns a Boolean by checking if the given string starts with the given prefix.
12.endsWith(suffix) -> Returns a Boolean by checking if the given string ends with the given suffix.
13.replace(old, new) -> Returns a String by replacing every occurance of the old substring with new substring in the string.
14.split(delimiter) -> Returns a List by seperating the string into chunks using the delimiter string.
15.indexOf(substring) -> Returns a Number by finding the index of a substring in the given string. Returns -1 if the substring is not present.
16.contains(substring) -> Returns a Boolean by checking if a substring is present in the given string.
17.toString() -> Provided for support. Returns the given string.
18.length() -> Returns the length of the string.

List functions:
1. init(list) -> Provided for support. Creates a new List object using an existing List.
2. length() -> Returns the length of the list.
3. append(value) -> Adds a value to the end of the list.
4. insert(value, index) -> Adds a value to the list at index and shifts the remaining list to match.
5. removeAt(index) -> Removes the value in the list at index and shifts the remaining list to match.
6. get(index) -> Provided for support. Returns the value in the list at index.
7. set(value, index) -> Provided for support. Sets the value at index to given value.
8. contains(value) -> Returns a Boolean by checking if the list contains a value.
9. indexOf(value) -> Returns the index of a value in the list.
10.clear() -> clears the list.
11.isEmpty() -> Returns a Boolean by checking if the list is empty.
12.toString() -> Returns the string representation of the list.
13.clone() -> Shallow copies the list to another list.
14.reverse() -> Reverses the list.
15.slice(start, end) -> Returns a check of the list from start to end position.
16.sort() -> Sorts the list. Uses quickSort by default.
17.quickSort() -> Performs QuickSort on the list.
18.swap(i, j) -> swaps the position of two values by index.

Math functions:
1. static sqrt(num) -> Returns the square root of a number.
2. static ln(num) -> Returns the natural log of a number.
3. static exp(num) -> Returns e raised to the power of num.
4. static pow(a, b) -> Returns a raised to the power of b.
5. static round(num, decimals) -> Rounds a number to a given number of decimals.
6. static round(num) -> Rounds a number to the nearest integer.
7. static abs(x) -> Returns the absolute value of a number.
8. static ceil(x) -> Returns the nearest integer greater than a number.
9. static floor(x) -> Returns the nearest integer lesser than a number.
10.static max(a, b) -> Returns the greater number between a and b.
11.static min(a, b) -> Returns the lesser number between a and b.
12.static sign(x) -> 1 for positive, -1 for negative, 0 for 0.
13.static clamp(x, min, max) -> Clamps a number between two bounds.
14.static mod(a, b) -> Returns the absolute mod of two numbers. Different from % operator.
15.static toRadians(deg) -> Converts to Radian value.
16.static toDegrees(rad) -> Converts to degree value.
17.static sin(x) -> Gives sin of an angle in radians.
18.static cos(x) -> Gives cos of an angle in radians.
19.static tan(x) -> Gives tan of an angle in radians.
20.static asin(x) -> Gives the angle which would produce the sin value;
21.static acos(x) -> Gives the angle which would produce the cos value.
22.static atan(x) -> Gives the angle which would produce the tan value.


General Utility functions
==========================
1. instanceOf(instance, clazz) -> Checks if a object is the instance of a class.
2. isChildOf(child, parent) -> Check if a class extends another class directly or indirectly.
3. isClass(clazz) -> Checks if a value is a class.
4. isInstance(inst) -> Checks if a value is a instance.
5. getNamce(obj) -> Returns the original name of the class or instance.

Native Functions
==========================
1. asc(string) -> Returns the ascii of the first character in a string.
2. char(ascii) -> Returns the character corresponding to the given ascii.
3. clock() -> Returns the current system time in miliseconds.
4. input() -> Takes input from the console and return it as a string.
5. print(val) -> Prints value to the console.
6. println(val) -> Prints value to the console and moves to the next line.
7. println() -> Moves to the next line in console.
8. type(obj) -> Returns the type of a class, instance or function.
