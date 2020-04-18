// See https://docusaurus.io/docs/site-config.html for all the possible
// site configuration options.

const repoUrl = "https://github.com/scalameta/metaconfig";
const baseUrl = "/metaconfig/";
const features = [
  {
    title: "HOCON and JSON support",
    content: "Support user configuration in HOCON or JSON syntax",
    image: "https://i.imgur.com/goYdJhw.png",
    imageAlign: "left",
  },
  {
    title: "Command-line parsing",
    content:
      "Parse command-line arguments using the same solution as for reading user configuration files.",
    image: "https://i.imgur.com/goYdJhw.png",
    imageAlign: "right",
  },
  {
    title: "Documentation generation",
    content:
      "Generate comprehensive documentation of all supported configuration options, including example usages and deprecation notices. " +
      "Documentation for configuration options ",
    image: "https://i.imgur.com/goYdJhw.png",
    imageAlign: "left",
  },
];

const siteConfig = {
  title: "Metaconfig",
  tagline: "Library to build configurable applications",
  url: "https://scalameta.org/metaconfig",
  baseUrl: baseUrl,

  // Used for publishing and more
  projectName: "metaconfig",
  organizationName: "scalameta",

  // algolia: {
  //   apiKey: "586dbbac9432319747bfea750fab16cb",
  //   indexName: "scalameta_munit"
  // },

  gaTrackingId: "UA-140140828-1",

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    { doc: "getting-started", label: "Docs" },
    { href: repoUrl, label: "GitHub", external: true },
  ],

  // If you have users set above, you add it here:
  // users,

  /* path to images for header/footer */
  headerIcon: "img/scalameta-logo.png",
  footerIcon: "img/scalameta-logo.png",
  favicon: "img/favicon.ico",

  /* colors for website */
  colors: {
    primaryColor: "#058772",
    secondaryColor: "#045C4D",
  },

  customDocsPath: "website/target/docs",

  stylesheets: [baseUrl + "css/custom.css"],

  blogSidebarCount: "ALL",

  // This copyright info is used in /core/Footer.js and blog rss/atom feeds.
  copyright: `Copyright © ${new Date().getFullYear()} Scalameta`,

  highlight: {
    // Highlight.js theme to use for syntax highlighting in code blocks
    theme: "github",
  },

  /* On page navigation for the current documentation page */
  onPageNav: "separate",

  /* Open Graph and Twitter card images */
  ogImage: "img/scalameta-logo.png",
  twitterImage: "img/scalameta-logo.png",

  editUrl: `${repoUrl}/edit/master/docs/`,

  repoUrl,
  features: features,
};

module.exports = siteConfig;
