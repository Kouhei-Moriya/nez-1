Nez : open grammar and tools
===================

Nez is an open grammar specification language. 
Once you write a grammar for complex data or computer languages, 
you can use it anywhere for various purposes including pattern matchers, 
transformers, interpreters, compilers and other language tools.

Features
--------

* Simple and declarative grammar specification
    * Based on parsing expression grammars
    * No action code, but allowing context-sensitive parsing (in part)
    * Flexible tree constructions with capturing-style notations
    * Open grammar repository is available
* Broad and extensible parser availability
    * Fast parser (constant-space packrat parsing and optimization)
    * Multiple parser availability: parser generation, grammar translation, and parser virtual machine (interpreters)

Nez named after "nezumi", rats in Japanese. 
As name implies, Nez has been inspired by packrat parsing and Rats! parser generator. 

Quick Start
-----------

To use the nez command, download the nez.jar file:

```
$ curl -O http://nez-peg.github.io/download/nez.jar
$ sudo nez.jar /usr/local/lib/nez.jar
$ alias nez='java -jar /usr/local/lib/nez.jar'
```

Also, it is good idea to put the alias setting in your `.bash_profile` or something like it. 

To test the installation, enter nez and check its version: 

```
$ nez
Nez-1.0-886 (beta) on JvM-1.8.0_xx
Usage: ...


```

## Nez Grammar at Glance

Nez grammar describes both syntactic constructs for the input and data structures for the output. 

The syntactic part is based on parsing expression grammars. 

```peg
Expression = Sum
Sum = Product (( '+' / '-' ) Product )*
Product = Value (( '*' #Mul / '/' #Div ) Value)*
Value = [0-9]+ / '(' Expression ')'
```

The data structure part is additionally specified by PCRE-style capturing notations. 

```nez
Expression = Sum
Sum = Product {$left ( '+' #Add / '-' #Sub ) $right(Product) }*
Product = Value {$left ( '*' #Mul / '/' #Div ) $right(Value) }*
Value = { [0-9]+ #Int } / '(' Expression ')'
```



## Generating a Parser


## Development
Nez is originally developed by [Kimio Kuramitsu](http://kuramitsulab.github.io/) with his graduate students in Yokohama National University, JAPAN. 

You are welcome to contribute code. 
Unless your contribution is really trivial you should get in touch with me first — this can prevent wasted effort on both sides. 
You can send code both as a patch or a GitHub pull request.

Note that Nez is still very much work in progress. 
There are no compatibility guarantees while the beta version.


