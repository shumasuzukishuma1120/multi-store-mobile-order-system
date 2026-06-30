- [JUnit 自分用メモ](#junit-自分用メモ)
  - [mockito](#mockito)
    - [mockitoとは](#mockitoとは)
    - [mockの作成方法](#mockの作成方法)
      - [Mockito.mock()メソッドを使う方法](#mockitomockメソッドを使う方法)
      - [@Mockアノテーションを使う方法](#mockアノテーションを使う方法)
    - [mockの振る舞いを設定する２つの方法](#mockの振る舞いを設定する２つの方法)
      - [when-then方式](#when-then方式)
      - [do-when方式](#do-when方式)
    - [staticメソッドのモック化](#staticメソッドのモック化)
      - [　呼び出し順序の検証](#呼び出し順序の検証)
      - [スパイの作成方法](#スパイの作成方法)
      - [spy()メソッドを使う方法](#spyメソッドを使う方法)
      - [@Spyアノテーションを使う方法](#spyアノテーションを使う方法)
    - [注意点　](#注意点)
  - [SpringBootTest](#springboottest)
    - [SpringBootが提供するテスト用のアノテーション](#springbootが提供するテスト用のアノテーション)
    - [サービスの単体テスト](#サービスの単体テスト)
      - [サービスの例　](#サービスの例)
      - [サービスの単体テスト例](#サービスの単体テスト例)
    - [MockMVCによるコントローラの単体テスト](#mockmvcによるコントローラの単体テスト)
      - [テストメソッドの典型的な構造](#テストメソッドの典型的な構造)
        - [疑似的なリクエストの構築](#疑似的なリクエストの構築)
        - [パラメータ/リクエストボディ/HTTPヘッダー](#パラメータリクエストボディhttpヘッダー)
      - [セッション属性とフラッシュ属性](#セッション属性とフラッシュ属性)
        - [コントローラでのそれらの操作](#コントローラでのそれらの操作)
      - [コントローラの単体テスト例](#コントローラの単体テスト例)
    - [注意点　](#注意点-1)

# JUnit 自分用メモ
## mockito

### mockitoとは

Unitテストではテストダブルは基本的にmockitoを使う。

when-then/do-whenであらわされるのがスタブ、verifyであらわされるのがモック。

### mockの作成方法

- `Mockito.mock()`メソッドを使う方法
- `@Mock`アノテーションを使う方法 ← 現在のデファクトスタンダード

#### Mockito.mock()メソッドを使う方法

```java
import static org.mockito.Mockito.*;

MyClass myClassMock = mock(MyClass.class);
//型　　    mock名 = mock(型.class);
```
モックをインラインセットアップで作成したいケースで利用する。

#### @Mockアノテーションを使う方法

```java
import org.mockito.Mock;
@Mock
MyClass myClassMock;  // ①テストに使う部品（モック）
// 型　　    mock名;

@InjectMocks
TargetService targetService;  // ②テストしたい本物のクラス（ここに①が自動で注入される）

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);  // ③これで①と②の準備・合体を行う
}  // @Mockが付与されたフィールドをモック化する


//import ～ .openMocks(this)までで一つのセット
//このように書いておくと、openMocks を呼んだ瞬間に、targetService の中に myClassMock を自動で差し込んで（注入して）くれる

//各テストケースの中でなにも宣言せずともmyClassMockがすぐ使える状態
```

### mockの振る舞いを設定する２つの方法

- when-then方式 ← 直感的にわかりやすい
- do-when方式 ※ do-when方式に優位性がある2つのケースは後述する

#### when-then方式

```java
when(myClassMock.method(anyString())).thenReturn("hoge");
//　when(モック名.method(引数)).thenReturn(返却値)

when(myClassMock.method(anyString())).thenReturn("hoge", "fuga", "piyo");
//このように書くと、最初の呼び出しでは"hoge"、2回目の呼び出しでは"fuga"、3回目以降の呼び出しでは"piyo"が返却される

```

#####　例外の設定

```java
when(myClassMock.method(anyString())).thenThrow(new RuntimeException("hoge"));
//          thenReturnの代わりにthenThrowを使うことで、例外をスローするように設定できる

when(myClassMock.method(anyString())).thenThrow(
    new RuntimeException("hoge"), 
    new IllegalArgumentException("fuga"));
//このように書くと、最初の呼び出しではRuntimeException("hoge")、2回目の呼び出しではIllegalArgumentException("fuga")がスローされる

```

#### do-when方式

```java
doReturn("hoge").when(myClassMock).method(anyString());
//　doReturn(返却値).when(モック名).method(引数);

doReturn("hoge", "fuga", "piyo").when(myClassMock).method(anyString());
//このように書くと、最初の呼び出しでは"hoge"、2回目の呼び出しでは"fuga"、3回目以降の呼び出しでは"piyo"が返却される
```   

#####　例外の設定
```java
doThrow(new RuntimeException("hoge")).when(myClassMock).method(anyString());
//          doReturnの代わりにdoThrowを使うことで、例外をスローするように設定できる

doThrow(
        new RuntimeException("hoge"), 
        new IllegalArgumentException("fuga"))
        .when(myClassMock).method(anyString());
```

### staticメソッドのモック化
スタティックメソッドのモック化は"mockito-inline"をという拡張機能を使うことで可能になる。

スタティックなメソッドをモック化するため、モック化は **@BeforeAll** が付与されたinitAll()メソッドの中で **一度だけ** 行うのが望ましい。

```java
// 見本のクラス
public class CalcUtil {
    public static int add(int x, int y) {
        return x + y;
    }
}

// テストコード
static void initAll(){
    //CalcUtilのstaticメソッドをモック化する
    MockedStatic<CalcUtil> mock = mockStatic(CalcUtil.class);
    //MonkedStatic<型名> 変数名 = mockStatic(型名.class);

    //モックの疑似的なふるまいをすべて設定する　（暗黙的セットアップ）
    mock.when(() -> CalcUtil.add(90, 10)).thenReturn(100);
    mock.when(() -> CalcUtil.add(90, 20)).thenReturn(110);
    mock.when(() -> CalcUtil.add(90, 30)).thenThrow(new RuntimeException("hoge"));
}

```
//スタティックメソッドのwhenの引数には **「引数をとらない」ラムダ式**を使用し、その中にスタティックメソッド呼び出しを記述する。

###　コミュニケーションベース検証（verify()）

テスト対象ユニットとモックの間のコミュニケーションの内容を検証するもの
例えば、「モックのどのメソッドが何回呼ばれたか」を検証する。

コミュニケーションベース検証にはMockitoクラスの**verify()**メソッドを使う。

####　呼び出し回数の検証

verifyの第一引数にはモックを指定し、第二引数には期待される呼び出し回数を指定する。

```java 
// これらの呼び出しで実際によばれた回数を検証する
myClassMock.method(anyString());

// 期待される呼び出し=1回
verify(myClassMock, times(1)).method(anyString());
// verify(モック名, times(期待される呼び出し回数)).method(引数);

//　期待される呼び出し=3回
verify(myClassMock, times(3)).method(anyString());

//　期待される呼び出し=最大5回
verify(myClassMock, atMost(5)).method(anyString());

//　期待される呼び出し=0回（一度も呼ばれないことを検証する）
verify(myClassMock, never()).method(anyString());
```

#### 　呼び出し順序の検証
Mockitoのインターフェースである**InOrder**を使うことで、モックの呼び出し順序を検証することができる。

```java
InOrder inOrder = inOrder(myClassMock);

inOrder.verify(myClassMock).method(1);
inOrder.verify(myClassMock).method(2);
inOrder.verify(myClassMock).method(3);
```
InOrder()を呼び出しInOrderを取得
次にInOrder.verify()を呼び出すことで、モックの呼び出し順序を検証することができる。


###　スパイ作成とふるまいの設定

すでに存在するオブジェクトに対してスパイを作成し、そのふるまいを設定することができる。
モックは作成直後はただの空のインスタンスだが、スパイは作成直後は元のオブジェクトと同じふるまいをする。
また、既存インスタンスの状態やふるまいを変更せずにテストの都合に合わせて特定の振る舞いのみ上書きできる

#### スパイの作成方法

クラス/インタフェースどちらからも作成可能だがスパイは実態を伴ったラッパーオブジェクトとして作成することに意味があるため基本的には**クラスから作成**

- spy()メソッドを使う方法
- @Spyアノテーションを使う方法

#### spy()メソッドを使う方法

```java
MyClass myClass = new MyClass(); //普通のインスタンス生成
//スパイはもとのインスタンスがあってその上に上書きするものだからいったん元のインスタンスを生成

MyClass spyMyClass = spy(myClass);
//型　　spy名 = spy(元のインスタンス);
```

#### @Spyアノテーションを使う方法

```java
@Spy
MyClass spyMyClass;　//これだけでインラインでも使える。　下とセットなら、どこでもすぐにspyMyClassが使える
// 型　　 spy名;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}
```　
 
###　spyの振る舞いを設定する方法 

spyの振る舞いを設定する方法はモックと同じで、when-then方式とdo-when方式の2種類があるが、スパイは元のインスタンスのふるまいを持っているため、when-then方式で設定すると元のインスタンスのメソッドが呼ばれてしまうことがある。
そのため、スパイの振る舞いを設定する場合は**do-when**方式を使うのが望ましい。

### スパイの使いどころ

スパイの目的は既存のインスタン層の振る舞いを変更せず、テストの都合に合わせて特定の振る舞いのみ上書きすることにある。

既存インスタンスの戻り値を返さないメソッドが何らかの**副作用**を発生させる可能性がある場合、対象の振る舞いをdoNuthing()で上書きすることで副作用を回避できる。

```java
doNothing().when(spy).process();
//doNothing()で副作用を回避する
```

process()が呼び出されても何も処理が行われないため、副作用の発生を抑止することができ、verify()で呼び出し回数の検証を行うことができる。

### 注意点　
これらのAPIを利用する際は一般的には次のようにスタティックインポート文を使用する

```java
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
```


## SpringBootTest

### SpringBootが提供するテスト用のアノテーション
- @SpringBootTest
- @WebMvcTest
- @MockBean
- @SpyBean

* @SpringBootTestと@WebMvcTestはサービスとコントローラの単体テストにりよされる
  * @SpringBootTestを付与するとSpringBootのアプリケーションコンテキストが起動される（重くなる）
  * @WebMvcTestを付与するとSpringBootのアプリケーションコンテキストは**起動されず**、コントローラのテストに必要なコンポーネントのみが起動される（軽くなる）

* @MockBeanと@SpyBeanはSpringBootのアプリケーションコンテキストにモックやスパイを登録するためのアノテーション
  * @MockBeanはモックを登録するためのアノテーション
  * @SpyBeanはスパイを登録するためのアノテーション

### サービスの単体テスト
SpringBootのサービスはPOJO（Plain Old Java Object）であるため、通常のJUnitテストと同様にテスト可能である。

ただし、**Bean, DIを使用している場合**は素のJUnit+Mockitoではテストが困難になるため、**SpringBootTest**を使う必要がある。

**トランザクション管理**の挙動をテストする場合はモックではなく本物のDAOをDB接続してテストを行う必要があり、この場合も**SpringBootTest**を使用する。

#### サービスの例　
``` java
@Service
public class PersonService {
    private static final Logger logger = LoggerFactory.getLogger(
            PersonService.class);

    // DAO（インジェクション）
    @Autowired
    private PersonDAO personDao;

    // コンストラクタ
    public PersonService(PersonDAO personDao) {
        this.personDao = personDao;
    }

    // サービスメソッド：人物を取得する
    public Person getPerson(int personId) {
        logger.info("[ PersonService#getPerson ]");
        Person person = personDao.find(personId);
        return person;
    }
    // サービスメソッド：全人物を取得する
    public List<Person> getPersonsAll() {
        logger.info("[ PersonService#getPersonsAll ]");
        List<Person> personList = personDao.findAll();
        return personList;
    }

    // サービスメソッド：人物を検索する（年齢下限をキーに）
    public List<Person> getPersonsByLowerAge(int lowerAge) {
        logger.info("[ PersonService#getPersonsByLowerAge ]");
        List<Person> personList = personDao.findByLowerAge(lowerAge);
        return personList;
    }

    // サービスメソッド：人物を追加する
    public Person createPerson(Person person) {
        logger.info("[ PersonService#createPerson ]");
        int maxPersonId = personDao.getMaxPersonId();
        person.setPersonId(maxPersonId + 1);
        personDao.save(person);
        return person;
    }

    // サービスメソッド：人物を削除する
    public int removePerson(Integer personId) {
        logger.info("[ PersonService#removePerson ]");
        return personDao.delete(personId);
    }

    // サービスメソッド：人物を更新する
    public int updatePerson(Person person) {
        logger.info("[ PersonService#updatePerson ]");
        int result = personDao.update(person);
        return result;
    }

    // サービスメソッド：人物の年齢を更新する
    public int updatePersonAge(Person person) {
        logger.info("[ PersonService#updatePersonAge ]");
        int result = personDao.updateAge(person.getPersonId(), person.getAge());
        return result;
    }
}
```


#### サービスの単体テスト例
```java
@SpringBootTest
public class PersonServiceTest {
    // テスト対象クラス（インジェクション）
    @Autowired
    private PersonService personService;

    // テスト対象クラスの呼び出し先（モック化対象）
    @MockBean
    private PersonDAO personDao; //←この時点でモックとしてDIされる

    @BeforeEach
    void setUp() {
        // モック化されたPersonDaoの振る舞いを設定する
        Person alice = new Person(1, "Alice", 25, "female");
        Person bob = new Person(2, "Bob", 35, "male");
        Person carol = new Person(3, "Carol", 30, "female");
        List<Person> all = Arrays.asList(alice, bob, carol);
        when(personDao.find(anyInt())).thenReturn(alice);
        when(personDao.findAll()).thenReturn(all);
        when(personDao.findByLowerAge(anyInt())).thenAnswer(i -> {
            int age = i.getArgument(0);
            return all.stream().filter(p -> age <= p.getAge()).collect(Collectors.toList());
        });
        when(personDao.save(any(Person.class))).thenReturn(4); // 新しいIDを返す
        when(personDao.delete(anyInt())).thenReturn(1); // 1件削除されたと返す
        when(personDao.update(any(Person.class))).thenReturn(1); // 1件更新されたと返す
        when(personDao.updateAge(anyInt(), anyInt())).thenReturn(1); // 年齢が更新されたと返す
    }

    @Test
    @DisplayName("PersonService#getPerson()のテスト")
    void test_GetPerson() {
        // テスト実行し、実測値を取得する
        Person actual = personService.getPerson(1);

        // 期待値を生成する
        Person expected = new Person(1, "Alice", 25, "female");

        // 期待値と実測値が一致しているかを検証する
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("PersonService#getPersonAll()のテスト")
    void test_getPersonsAll() {
        // テスト実行し、実測値を取得する
        List<Person> actual = personService.getPersonsAll();

        // 期待値を生成する
        Person alice = new Person(1, "Alice", 25, "female");
        Person bob = new Person(2, "Bob", 35, "male");
        Person carol = new Person(3, "Carol", 30, "female");
        List<Person> expected = Arrays.asList(alice, bob, carol);

        // 期待値と実測値が一致しているかを検証する
        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("PersonService#getPersonsByLowerAge()のテスト")
    void test_getPersonsByLowerAge() {
        // テスト実行し、実測値を取得する
        List<Person> actual = personService.getPersonsByLowerAge(27);

        // 期待値を生成する
        Person bob = new Person(2, "Bob", 35, "male");
        Person carol = new Person(3, "Carol", 30, "female");
        List<Person> expected = Arrays.asList(bob, carol);

        // 期待値と実測値が一致しているかを検証する
        assertIterableEquals(expected, actual);
    }

    @Test
    @DisplayName("PersonService#createPerson()のテスト")
    void test_createPerson() {
        // テスト実行する
        Person dave = new Person(4, "Dave", 23, "male");
        personService.createPerson(dave);

        // モックの指定されたメソッド呼び出しが一度だけ行われたことを検証する
        verify(personDao).save(dave);
    }

    @Test
    @DisplayName("PersonService#removePerson()のテスト")
    void test_removePerson() {
        // テスト実行し、実測値を取得する
        int actual = personService.removePerson(3);

        // 期待値と実測値が一致しているかを検証する
        assertEquals(1, actual); 

        // モックの指定されたメソッド呼び出しが一度だけ行われたことを検証する
        verify(personDao).delete(3);
    }

    @Test
    @DisplayName("PersonService#updatePerson()のテスト")
    void test_updatePerson() {
        // テスト実行し、実測値を取得する
        Person alice = new Person(1, "Alice", 26, "female");
        int actual = personService.updatePerson(alice);

        // 期待値と実測値が一致しているかを検証する
        assertEquals(1, actual); 

        // モックの指定されたメソッド呼び出しが一度だけ行われたことを検証する
        verify(personDao).update(alice);
    }

    @Test
    @DisplayName("PersonService#updatePersonAge()のテスト")
    void test_updatePersonAge() {
        // テスト実行し、実測値を取得する
        Person bob = new Person(2, "Bob", 36, "male");
        int actual = personService.updatePersonAge(bob);

        // 期待値と実測値が一致しているかを検証する
        assertEquals(1, actual); 

        // モックの指定されたメソッド呼び出しが一度だけ行われたことを検証する
        verify(personDao).updateAge(2, 36);
    }
}
```

### MockMVCによるコントローラの単体テスト

* MockMVC
  * Spring Bootのコントローラのテストに特化したフレームワークでSpringBootTestに内包されている
  * **MVC**や**REST API** におけるコントローラのテストも可能

* 主な機能
  * 疑似的なリクエストの送信　(MockMvcによって行われる)
  * 疑似的なリクエストの構築　(MockMvcRequestBuildersによって行われる)
  * レスポンス検証の実行　(ResultActionsによって行われる)
  * レスポンスの検証項目の設定（MockMvcResultMatchersによって行われる） 

#### テストメソッドの典型的な構造

```java
@Test
void test_XXX() throws Exception { 
    mockMvc.perform(post("/XXX"))　
            .param("param1", "value1")
            .param("param2", "value2")
            ............
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .andExpect(status().isOk())
            .andExpect(view().name("XXXPage"))
            .andExpect(model().hasNoErrors())
            ...............;
}
```
1. MockMVCのAPIが例外を創出するので、テストメソッドの宣言にthrows Exceptionを付与する
2. テストメソッドの中でまず、perform()を呼び出す
3. perform()の引数には、MockMvcRequestBuildersのget()やpost()などを使って疑似的なリクエストを構築する
4. パラメータ、リクエストボディ、ヘッダなどを設定する
5. perform()によってリクエストが送信され、その戻り値はResultActionsであり、andExpect()を使ってレスポンスの検証を行う
6. ResultActionsのandExpect()の引数には、MockMvcResultMatchersのstatus()やview()、model()などを使ってレスポンスの検証項目を設定する

##### 疑似的なリクエストの構築

perform()の引数には、MockMvcRequestBuildersのget()やpost()などを使って疑似的なリクエストを構築する。

| API       | 説明                                         |
| --------- | -------------------------------------------- |
| get()     | 指定されたURLへのGETリクエストを構築する     |
| post()    | 指定されたURLへのPOSTリクエストを構築する    |
| put()     | 指定されたURLへのPUTリクエストを構築する     |
| delete()  | 指定されたURLへのDELETEリクエストを構築する  |
| options() | 指定されたURLへのOPTIONSリクエストを構築する |

##### パラメータ/リクエストボディ/HTTPヘッダー

| API                                        | 説明                                                                                                                             |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| param(String, String)                      | 指定されたKeyとValueのパラメータに追加する(GETの場合URLのクエリパラメータとして追加され、POSTの場合リクエストボディに追加される) |
| params(MultiValueMap<String, String>)      | 複数のKeyとValueのパラメータに追加する(GETの場合URLのクエリパラメータとして追加され、POSTの場合リクエストボディに追加される)     |
| queryParam(String, String)                 | 指定されたキーと値をクエリパラメータに追加する（GET限定）                                                                        |
| queryParams(MultiValueMap<String, String>) | 複数のキーと値をクエリパラメータに追加する（GET限定）                                                                            |
| content(String)                            | リクエストボディを設定する                                                                                                       |
| header(String, String)                     | HTTPヘッダーを設定する                                                                                                           |
| headers(HttpHeaders)                       | 複数のHTTPヘッダーを設定する                                                                                                     |
| redirectedUrl(String)                      | リダイレクト先のURLを設定する                                                                                                    |
| contentType(MediaType)                     | リクエストのContent-Typeを設定する                                                                                               |

#### セッション属性とフラッシュ属性

SpringBootには、コンテキスト情報を保持するためのスコープが存在する
* リクエストスコープ
* セッションスコープ
* フラッシュスコープ

各スコープではkey-value形式で情報を保持することができ、これらは属性と呼ばれる

##### コントローラでのそれらの操作

| API | 説明 |
| --- | --- |
| sessionAttr(String, Object) | 指定されたkeyとvalueでセッション属性を設定する |
| sessionAttrs(Map<String, Object>) | 指定されたkeyとvalueのマップでセッション属性を設定する |
| flashAttr(String, Object) | 指定されたkeyとvalueでフラッシュ属性を設定する |
| flashAttrs(Map<String, Object>) | 指定されたkeyとvalueのマップでフラッシュ属性を設定する |

```java
mockMvc.perform(post("/XXX"))
        .sessionAttr("personSession", personSession) // セッション属性を追加する
        .flashAttr("personFlash", personFlash) // フラッシュ属性を追加する
```

#####　レスポンス検証項目の設定
perform()によって送信されたリクエストのレスポンスを検証するために、ResultActionsのandExpect()を使ってレスポンスの検証項目を設定する。

レスポンスの以下の項目を検証の対象にできる
* レスポンスのステータスコード
* HTTPヘッダー
* レスポンスボディ
* SpringBootが管理するビュー、モデル、セッションスコープの属性

| API | 説明 |
| --- | --- |
| status() | レスポンスのステータスコードを検証する |
| view() | SpringBootが管理するビューを検証する |
| model() | SpringBootが管理するモデルを検証する |
| request() | SpringBootが管理するリクエスト属性を検証する |
| content() | レスポンスボディを検証する |
| jsonPath(String) | レスポンスボディのJSONを検証する |
| header(String, Object) | HTTPヘッダーを検証する |
| header() | HTTPヘッダーを検証する |
| session() | SpringBootが管理するセッション属性を検証する |



#### コントローラの単体テスト例
```java
@WebMvcTest(PersonController.class)
public class PersonControllerTest {
    // MockMvcをインジェクションする
    @Autowired
    private MockMvc mockMvc;

    // テスト対象クラスの呼び出し先（モック化対象）
    @MockBean
    private PersonService personService; //すでにモック化してる。

    @Test
    @DisplayName("トップ画面への遷移をテストする")
    void test_ViewPersonList() throws Exception {
        // モック化されたPersonServiceの振る舞いを設定する
        Person alice = new Person(1, "Alice", 25, "female");
        Person bob = new Person(2, "Bob", 35, "male");
        Person carol = new Person(3, "Carol", 30, "female");
        List<Person> personList = Arrays.asList(alice, bob, carol);
        when(personService.getPersonsAll()).thenReturn(personList);

        // テストを実行し、検証する
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/viewList"));

        // テストを実行し、検証する（リダイレクト後）
        mockMvc.perform(get("/viewList"))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonTablePage"))
                .andExpect(model().attributeExists("personList"))
                .andExpect(model().attribute("personList", personList));
    }

    @Test
    @DisplayName("入力画面への遷移をテストする")
    void test_toCreate() throws Exception {
        // テストを実行し、検証する
        mockMvc.perform(post("/toCreate"))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonInputPage"))
                .andExpect(request().sessionAttributeDoesNotExist("personSession"));
    }

    @Test
    @DisplayName("新規人物作成と確認画面への遷移をテストする")
    void test_toConfirm() throws Exception {
        PersonSession personSession = new PersonSession("Dave", 23, "male");

        // テストを実行し、検証する
        mockMvc.perform(post("/toConfirm")
                .param("personName", "Dave")
                .param("age", "23")
                .param("gender", "male")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonUpdatePage"))
                .andExpect(model().hasNoErrors())
                .andExpect(request().sessionAttribute("personSession", personSession));
    }

    @Test
    @DisplayName("新規人物作成における入力エラーをテストする")
    void test_toConfirm_ValidationError() throws Exception {
        // テストを実行し、検証する
        mockMvc.perform(post("/toConfirm")
                .param("personName", "DaveDaveDaveDaveDaveDave") // 20字超
                .param("age", "10") // 20未満
                .param("gender", "") // 空文字
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonInputPage"))
                .andExpect(model().attributeHasFieldErrors(
                        "personSession", "personName", "age", "gender"));
    }

    @Test
    @DisplayName("人物の更新をテストする")
    void test_UpdatePerson() throws Exception {
        // モック化されたPersonServiceの振る舞いを設定する
        Person alice = new Person(1, "Alice", 26, "female");
        Person bob = new Person(2, "Bob", 35, "male");
        Person carol = new Person(3, "Carol", 30, "female");
        List<Person> personList = Arrays.asList(alice, bob, carol);
        when(personService.getPersonsAll()).thenReturn(personList);

        // 入力値をセットアップする
        PersonSession personSession = new PersonSession(1, "Alice", 26, "female");

        // テストを実行し、検証する
        mockMvc.perform(post("/update")
                .sessionAttr("personSession", personSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/viewList"));

        // テストを実行し、検証する（リダイレクト後）
        mockMvc.perform(get("/viewList"))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonTablePage"))
                .andExpect(model().attributeExists("personList"))
                .andExpect(model().attribute("personList", personList));

        // モックの指定されたメソッド呼び出しが一度だけ行われたことを検証する
        verify(personService).updatePerson(any(Person.class));

        // 新規作成画面に移動したとき、セッション属性が削除されていることを検証する
        mockMvc.perform(post("/toCreate"))
               .andExpect(request().sessionAttributeDoesNotExist("personSession"));
    }

    @Test
    @DisplayName("人物の追加をテストする")
    void test_AddPerson() throws Exception {
        // モック化されたPersonServiceの振る舞いを設定する
        Person alice = new Person(1, "Alice", 25, "female");
        Person bob = new Person(2, "Bob", 35, "male");
        Person carol = new Person(3, "Carol", 30, "female");
        Person dave = new Person(4, "Dave", 23, "male");
        List<Person> personList = Arrays.asList(alice, bob, carol, dave);
        when(personService.getPersonsAll()).thenReturn(personList);

        // 入力値をセットアップする
        PersonSession personSession = new PersonSession("Dave", 23, "male");

        // テストを実行し、検証する
        mockMvc.perform(post("/update")
                .sessionAttr("personSession", personSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/viewList"));

        // テストを実行し、検証する（リダイレクト後）
        mockMvc.perform(get("/viewList"))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonTablePage"))
                .andExpect(model().attributeExists("personList"))
                .andExpect(model().attribute("personList", personList));

        // モックの指定されたメソッド呼び出しが一度だけ行われたことを検証する
        Person dave2 = new Person("Dave", 23, "male");
        verify(personService).createPerson(dave2);

        // 新規作成画面に移動したとき、セッション属性が削除されていることを検証する
        mockMvc.perform(post("/toCreate"))
                .andExpect(request().sessionAttributeDoesNotExist("personSession"));
    }

    @Test
    @DisplayName("人物の編集をテストする")
    void test_EditPerson() throws Exception {
        // モック化されたPersonServiceの振る舞いを設定する
        Person alice = new Person(1, "Alice", 25, "female");
        when(personService.getPerson(1)).thenReturn(alice);

        // テストを実行し、検証する
        mockMvc.perform(post("/edit")
                .param("personId", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonInputPage"))
                .andExpect(model().attributeHasNoErrors("personSession"));
    }

    @Test
    @DisplayName("人物の削除をテストする")
    public void test_RemovePerson() throws Exception {
        // モック化されたPersonServiceの振る舞いを設定する
        Person alice = new Person(1, "Alice", 25, "female");
        Person bob = new Person(2, "Bob", 35, "male");
        List<Person> personList = Arrays.asList(alice, bob);
        when(personService.getPersonsAll()).thenReturn(personList);

        // テストを実行し、検証する
        mockMvc.perform(post("/remove")
                .param("personId", "3")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/viewList"));

        // テストを実行し、検証する（リダイレクト後）
        mockMvc.perform(get("/viewList"))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonTablePage"))
                .andExpect(model().attributeExists("personList"))
                .andExpect(model().attribute("personList", personList));

        // モックの指定されたメソッド呼び出しが一度だけ行われたことを検証する
        verify(personService).removePerson(3);
    }

    @Test
    @DisplayName("入力画面への遷移をテストする")
    public void test_Back() throws Exception {
        // テストを実行し、検証する
        mockMvc.perform(get("/back"))
                .andExpect(status().isOk())
                .andExpect(view().name("PersonInputPage"));
    }
}
```

### 注意点　
これらのAPIを利用する際は一般的には次のようにスタティックインポート文を使用する

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
```