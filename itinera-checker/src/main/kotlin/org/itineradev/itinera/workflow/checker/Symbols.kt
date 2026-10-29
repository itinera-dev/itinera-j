package org.itineradev.itinera.workflow.checker

/**
 * A class as the checker reads it from a compiler, javac's or KSP's: what the run-time reader would
 * read of it through reflection.
 */
internal interface ClassSymbol : Annotated {
    /** Its binary name, as `Class.getName` writes it, such as `com.shop.Orders$Charge`. */
    val name: String

    /** Its simple name. */
    val simpleName: String

    /** Where the compiler reports a mistake in it. */
    val origin: Any

    /** Whether it is an interface. */
    val isInterface: Boolean

    /** Whether it is abstract, as an interface is. */
    val isAbstract: Boolean

    /** Whether it is a record. */
    val isRecord: Boolean

    /** Whether it is an enum, or the class of one of an enum's constants. */
    val isEnum: Boolean

    /** Whether Kotlin compiled it. */
    val isKotlin: Boolean

    /**
     * For a Kotlin class, whether itinera-kotlin reads it as data: a data class whose only fields
     * are the `val` properties of its primary constructor, or a `data object` whose fields are all
     * final; `null` when the compiler cannot say, as javac cannot for a compiled Kotlin class.
     */
    val kotlinData: Boolean?

    /** The names of its type parameters, in order. */
    val typeParameters: List<String>

    /** Its superclass, if it has one. */
    val superclass: ClassSymbol?

    /** Its direct supertypes, superclass first, as it names them, with their type arguments. */
    val supertypes: List<ClassUse>

    /** Its constructors, as the JVM holds them, without those the compiler adds. */
    val constructors: List<FunctionSymbol>

    /** The methods it declares itself, of every visibility, without those the compiler adds. */
    val methods: List<FunctionSymbol>

    /**
     * Its public methods, its own and those it inherits, as `Class.getMethods` gives them: one for
     * each method an override replaces.
     */
    val publicMethods: List<FunctionSymbol>

    /** Whether each field it declares itself and not as `static` is final. */
    val instanceFields: List<Boolean>

    /** Each annotation of the repeatable class [name] that marks it, in the order written. */
    fun annotations(name: String): List<AnnotationSymbol>

    /** Whether it is the class [name], or extends or implements it. */
    fun isSubtypeOf(name: String): Boolean
}

/** A method or a constructor, as the checker reads it from a compiler. */
internal interface FunctionSymbol : Annotated {
    /** The class that declares it. */
    val owner: ClassSymbol

    /** Whether it is a constructor. */
    val isConstructor: Boolean

    /** Its name on the JVM, which Kotlin changes for an `internal` method. */
    val jvmName: String

    /** Its name as its declaration writes it. */
    val name: String

    /** Whether it is public on the JVM, as Kotlin's `internal` is. */
    val isPublic: Boolean

    /** Whether it is private. */
    val isPrivate: Boolean

    /** Whether it is static. */
    val isStatic: Boolean

    /** Whether it is a `suspend` function. */
    val isSuspend: Boolean

    /** Its declared return type, [VoidUse] for a constructor. */
    val returns: TypeUse

    /**
     * The class the run-time reader reads it to return: in a Java class, the erased class Java
     * reflection gives, and in a Kotlin class, Kotlin's class of its return type, or `Object` for a
     * type variable; `null` for `void`, a primitive type and an array.
     */
    val returnClass: ClassSymbol?

    /**
     * The simple name of what the run-time reader reads it to return, such as `long` or `Outcome`.
     */
    val returnName: String

    /**
     * The classes the run-time reader reads as its return type's type arguments, by their binary
     * names, such as `com.shop.Payments` for `Provided<Payments>`, each `null` when it names no
     * class, as a type variable, a wildcard and a star projection do not.
     */
    val returnArguments: List<String?>

    /** Its parameters, as its declaration writes them. */
    val parameters: List<ParameterSymbol>

    /** Whether it captures what its enclosing code holds, as an inner class's constructor does. */
    val capturesEnclosing: Boolean

    /** Where the compiler reports a mistake in it. */
    val origin: Any

    /** Whether it overrides [other], as a method of a subclass overrides one of its superclass. */
    fun overrides(other: FunctionSymbol): Boolean
}

/** A parameter of a method or a constructor. */
internal interface ParameterSymbol : Annotated {
    /** Its name. */
    val name: String

    /** Its declared type. */
    val type: TypeUse

    /**
     * Its erased class on the JVM, named as `Class.getName` names it, such as `long`,
     * `java.util.List` or `[Ljava.lang.String;`: what the run-time reader compares.
     */
    val jvmClass: String

    /** Its erased class on the JVM, if that is a class rather than a primitive type or an array. */
    val erasedClass: ClassSymbol?
}

/** An annotation, with the value of each of its elements, written or by default. */
internal interface AnnotationSymbol {
    /** The string [element] holds. */
    fun string(element: String): String

    /** The strings [element] holds. */
    fun strings(element: String): List<String>

    /** The boolean [element] holds. */
    fun boolean(element: String): Boolean

    /** The integer [element] holds. */
    fun int(element: String): Int

    /** The class [element] holds. */
    fun type(element: String): ClassSymbol

    /** The classes [element] holds. */
    fun types(element: String): List<ClassSymbol>
}

/** A declaration annotations can mark. */
internal interface Annotated {
    /** The annotation of the class [name] that marks it, if any. */
    fun annotation(name: String): AnnotationSymbol?

    /** Whether the annotation of the class [name] marks it. */
    fun isMarked(name: String): Boolean = annotation(name) != null
}
