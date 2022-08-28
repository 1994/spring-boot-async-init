import com.github.nineteen.async.init.test.AsyncInitContextLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(loader = AsyncInitContextLoader.class)
public class TestApplication {

}
