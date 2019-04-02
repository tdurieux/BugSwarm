<?php

class Foo {                           // NOK - depends on 12 classes

    use MyTrait;

    /**
     * @var T1
     */
     public $a1;                      // coupled to T1

    /**
     * @var T2
     */
     protected $a2;                   // coupled to T2

    /**
     * @var T3
     */
     private $a3;                     // coupled to T3

    /**
     * @var Boolean
     */
     private $a3;

    /**
     * @var T4
     */
     const A4 = 1;

    /**
     * @param T6 $a
     * @param T7
     *
     * @return T5
     */
     public function  f(T6 $a, $b) {  // coupled to T5, T6, T7
       $result = new T8();            // coupled to T8
       $localVar = new T9();          // coupled to T9

       return $result;
     }

    /**
     * @throws T10
     * @param T11
     *
     * @return T12|null
     */
     public function g(T11 $p) {      // coupled to T10, T11, T12
       if ($p->a) {
         throw new T10();
       }
       return new T11();
     }

     /**
      * @param T12[]
      */
     public function h($p) {
      return doSomething($p);
     }

     /**
      * No doc
      */
      public function i() {
       return new static::Foo();      // Not supported
      }

     /**
      * @return \T13
      */
      public function i() {
       return new namespace\T13 ();    // coupled to T13
      }

     /**
      * No doc
      */
      public function k() {
        $class = "T14";
        return new $class();          // Not supported
      }
}

class Bar {       // OK - depends on 2 classes

  /**
   * @var T1
   */
   public $a1;    // coupled to T1

  /**
   * @var T2
   */
   public $a2;    // coupled to T2

  /**
   * @param array
   *
   * @return string
   */
   public function f(array $p = array()) {
     return $p->toString();
   }
}
